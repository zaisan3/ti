package com.zaisan.ti.notify.spring.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ErrorHandler;

import redis.clients.jedis.Jedis;

import com.zaisan.ti.notify.ChannelTopic;
import com.zaisan.ti.notify.Topic;
import com.zaisan.ti.notify.maptable.MapTableOperator;
import com.zaisan.ti.notify.redis.JedisProvider;
import com.zaisan.ti.notify.util.ByteArrayWrapper;

/**
 * copy from spring-data-redis
 * @author jiajia.sang
 *
 */
public class RedisMessageListenerContainer implements InitializingBean, DisposableBean, BeanNameAware, SmartLifecycle{

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Default thread name prefix: "RedisListeningContainer-".
	 */
	public static final String DEFAULT_THREAD_NAME_PREFIX = ClassUtils.getShortName(RedisMessageListenerContainer.class)
			+ "-";

	/**
	 * The default recovery interval: 5000 ms = 5 seconds.
	 */
	public static final long DEFAULT_RECOVERY_INTERVAL = 5000;

	/**
	 * The default subscription wait time: 2000 ms = 2 seconds.
	 */
	public static final long DEFAULT_SUBSCRIPTION_REGISTRATION_WAIT_TIME = 2000L;

	private long initWait = TimeUnit.SECONDS.toMillis(5);

	private Executor subscriptionExecutor;

	private Executor taskExecutor;

	
	private JedisOperator jedisOperator;

	private String beanName;

	
	private ErrorHandler errorHandler;

	private final Object monitor = new Object();
	// whether the container is running (or not)
	private volatile boolean running = false;
	// whether the container has been initialized
	private volatile boolean initialized = false;
	// whether the container uses a connection or not
	// (as the container might be running but w/o listeners, it won't use any resources)
	private volatile boolean listening = false;

	private volatile boolean manageExecutor = false;

	// lookup maps
	// to avoid creation of hashes for each message, the maps use raw byte arrays (wrapped to respect the equals/hashcode
	// contract)

	// lookup map between patterns and listeners
	private final Map<ByteArrayWrapper, Collection<NotifyMessageListener>> patternMapping = new ConcurrentHashMap<ByteArrayWrapper, Collection<NotifyMessageListener>>();
	// lookup map between channels and listeners
	private final Map<ByteArrayWrapper, Collection<NotifyMessageListener>> channelMapping = new ConcurrentHashMap<ByteArrayWrapper, Collection<NotifyMessageListener>>();
	// lookup map between listeners and channels
	private final Map<NotifyMessageListener, Set<Topic>> listenerTopics = new ConcurrentHashMap<NotifyMessageListener, Set<Topic>>();

	private final SubscriptionTask subscriptionTask = new SubscriptionTask();

	private volatile RedisSerializer<String> serializer = new StringRedisSerializer();

	private long recoveryInterval = DEFAULT_RECOVERY_INTERVAL;

	private long maxSubscriptionRegistrationWaitingTime = DEFAULT_SUBSCRIPTION_REGISTRATION_WAIT_TIME;
	
	private MapTableOperator mapTableOperator=new MapTableOperator();

	public void afterPropertiesSet() {
		if (taskExecutor == null) {
			manageExecutor = true;
			taskExecutor = createDefaultTaskExecutor();
		}

		if (subscriptionExecutor == null) {
			subscriptionExecutor = taskExecutor;
		}

		initialized = true;
	}

	/**
	 * Creates a default TaskExecutor. Called if no explicit TaskExecutor has been specified.
	 * <p>
	 * The default implementation builds a {@link org.springframework.core.task.SimpleAsyncTaskExecutor} with the
	 * specified bean name (or the class name, if no bean name specified) as thread name prefix.
	 * 
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor#SimpleAsyncTaskExecutor(String)
	 */
	protected TaskExecutor createDefaultTaskExecutor() {
		String threadNamePrefix = (beanName != null ? beanName + "-" : DEFAULT_THREAD_NAME_PREFIX);
		return new SimpleAsyncTaskExecutor(threadNamePrefix);
	}

	public void destroy() throws Exception {
		initialized = false;

		stop();

		if (manageExecutor) {
			if (taskExecutor instanceof DisposableBean) {
				((DisposableBean) taskExecutor).destroy();

				if (logger.isDebugEnabled()) {
					logger.debug("Stopped internally-managed task executor");
				}
			}
		}
	}

	public boolean isAutoStartup() {
		return true;
	}

	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	public int getPhase() {
		// start the latest
		return Integer.MAX_VALUE;
	}

	public boolean isRunning() {
		return running;
	}

	public void start() {
		if (!running) {
			running = true;
			// wait for the subscription to start before returning
			// technically speaking we can only be notified right before the subscription starts
			synchronized (monitor) {
				lazyListen();
				if (listening) {
					try {
						// wait up to 5 seconds for Subscription thread
						monitor.wait(initWait);
					} catch (InterruptedException e) {
						// stop waiting
						Thread.currentThread().interrupt();
						running = false;
						return;
					}
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Started RedisMessageListenerContainer");
			}
		}
	}

	public void stop() {
		if (isRunning()) {
			running = false;
			subscriptionTask.cancel();
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Stopped RedisMessageListenerContainer");
		}
	}

	/**
	 * Process a message received from the provider.
	 * 
	 * @param message
	 * @param pattern
	 */
	protected void processMessage(NotifyMessageListener listener, Message message, byte[] pattern) {
		executeListener(listener, message, pattern);
	}

	/**
	 * Execute the specified listener.
	 * 
	 * @see #handleListenerException
	 */
	protected void executeListener(NotifyMessageListener listener, Message message, byte[] pattern) {
		try {
			jedisOperator.execute(new RedisCallback<Long>() {
				@Override
				public Long doInRedis(Jedis jedis) {
					listener.onMessage(message, pattern,jedis);
					return null;
				}
			});
		} catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	/**
	 * Return whether this container is currently active, that is, whether it has been set up but not shut down yet.
	 */
	public final boolean isActive() {
		return initialized;
	}

	/**
	 * Handle the given exception that arose during listener execution.
	 * <p>
	 * The default implementation logs the exception at error level. This can be overridden in subclasses.
	 * 
	 * @param ex the exception to handle
	 */
	protected void handleListenerException(Throwable ex) {
		if (isActive()) {
			// Regular case: failed while active.
			// Invoke ErrorHandler if available.
			invokeErrorHandler(ex);
		} else {
			// Rare case: listener thread failed after container shutdown.
			// Log at debug level, to avoid spamming the shutdown logger.
			logger.debug("Listener exception after container shutdown", ex);
		}
	}

	/**
	 * Invoke the registered ErrorHandler, if any. Log at error level otherwise.
	 * 
	 * @param ex the uncaught error that arose during message processing.
	 * @see #setErrorHandler
	 */
	protected void invokeErrorHandler(Throwable ex) {
		if (this.errorHandler != null) {
			this.errorHandler.handleError(ex);
		} else if (logger.isWarnEnabled()) {
			logger.warn("Execution of message listener failed, and no ErrorHandler has been set.", ex);
		}
	}

	/**
	 * Returns the jedisFactory.
	 * 
	 * @return Returns the jedisFactory
	 */
	public JedisOperator getJedisOperator() {
		return jedisOperator;
	}

	/**
	 * @param jedisFactory The jedisFactory to set.
	 */
	public void setJedisProvider(JedisProvider jedisProvider) {
		jedisOperator= new DefaultJedisOperator();
		jedisOperator.setJedisProvider(jedisProvider);
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * Sets the task executor used for running the message listeners when messages are received. If no task executor is
	 * set, an instance of {@link SimpleAsyncTaskExecutor} will be used by default. The task executor can be adjusted
	 * depending on the work done by the listeners and the number of messages coming in.
	 * 
	 * @param taskExecutor The taskExecutor to set.
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Sets the task execution used for subscribing to Redis channels. By default, if no executor is set, the
	 * {@link #setTaskExecutor(Executor)} will be used. In some cases, this might be undersired as the listening to the
	 * connection is a long running task.
	 * <p>
	 * Note: This implementation uses at most one long running thread (depending on whether there are any listeners
	 * registered or not) and up to two threads during the initial registration.
	 * 
	 * @param subscriptionExecutor The subscriptionExecutor to set.
	 */
	public void setSubscriptionExecutor(Executor subscriptionExecutor) {
		this.subscriptionExecutor = subscriptionExecutor;
	}

	/**
	 * Sets the serializer for converting the {@link Topic}s into low-level channels and patterns. By default,
	 * {@link StringRedisSerializer} is used.
	 * 
	 * @param serializer The serializer to set.
	 */
	public void setTopicSerializer(RedisSerializer<String> serializer) {
		this.serializer = serializer;
	}

	/**
	 * Set an ErrorHandler to be invoked in case of any uncaught exceptions thrown while processing a Message. By default
	 * there will be <b>no</b> ErrorHandler so that error-level logging is the only result.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Attaches the given listeners (and their topics) to the container.
	 * <p>
	 * Note: it's possible to call this method while the container is running forcing a reinitialization of the container.
	 * Note however that this might cause some messages to be lost (while the container reinitializes) - hence calling
	 * this method at runtime is considered advanced usage.
	 * 
	 * @param listeners map of message listeners and their associated topics
	 */
	public void setMessageListeners(Map<? extends NotifyMessageListener, Collection<? extends Topic>> listeners) {
		initMapping(listeners);
	}

	/**
	 * Adds a message listener to the (potentially running) container. If the container is running, the listener starts
	 * receiving (matching) messages as soon as possible.
	 * 
	 * @param listener message listener
	 * @param topics message listener topic
	 */
	public void addMessageListener(NotifyMessageListener listener, Collection<? extends Topic> topics) {
		addListener(listener, topics);
		lazyListen();
	}

	/**
	 * Adds a message listener to the (potentially running) container. If the container is running, the listener starts
	 * receiving (matching) messages as soon as possible.
	 * 
	 * @param listener message listener
	 * @param topic message topic
	 */
	public void addMessageListener(NotifyMessageListener listener, Topic topic) {
		addMessageListener(listener, Collections.singleton(topic));
	}



	private void initMapping(Map<? extends NotifyMessageListener, Collection<? extends Topic>> listeners) {
		// stop the listener if currently running
		if (isRunning()) {
			subscriptionTask.cancel();
		}

		patternMapping.clear();
		channelMapping.clear();
		listenerTopics.clear();

		if (!CollectionUtils.isEmpty(listeners)) {
			for (Map.Entry<? extends NotifyMessageListener, Collection<? extends Topic>> entry : listeners.entrySet()) {
				addListener(entry.getKey(), entry.getValue());
			}
		}

		// resume activity
		if (initialized) {
			start();
		}
	}

	/**
	 * Method inspecting whether listening for messages (and thus using a thread) is actually needed and triggering it.
	 */
	private void lazyListen() {
		boolean debug = logger.isDebugEnabled();
		boolean started = false;

		if (isRunning()) {
			if (!listening) {
				synchronized (monitor) {
					if (!listening) {
						if (channelMapping.size() > 0) {
							subscriptionExecutor.execute(subscriptionTask);
							listening = true;
							started = true;
						}
					}
				}
				if (debug) {
					if (started) {
						logger.debug("Started listening for Redis messages");
					} else {
						logger.debug("Postpone listening for Redis messages until actual listeners are added");
					}
				}
			}
		}
	}

	private void addListener(NotifyMessageListener listener, Collection<? extends Topic> topics) {
		Assert.notNull(listener, "a valid listener is required");
		Assert.notEmpty(topics, "at least one topic is required");

		List<byte[]> channels = new ArrayList<byte[]>(topics.size());

		boolean trace = logger.isTraceEnabled();

		// add listener mapping
		Set<Topic> set = listenerTopics.get(listener);
		if (set == null) {
			set = new CopyOnWriteArraySet<Topic>();
			listenerTopics.put(listener, set);
		}
		set.addAll(topics);

		for (Topic topic : topics) {

			ByteArrayWrapper holder = new ByteArrayWrapper(serializer.serialize(topic.getTopic()));

			if (topic instanceof ChannelTopic) {
				Collection<NotifyMessageListener> collection = channelMapping.get(holder);
				if (collection == null) {
					collection = new CopyOnWriteArraySet<NotifyMessageListener>();
					channelMapping.put(holder, collection);
				}
				collection.add(listener);
				channels.add(holder.getArray());

				if (trace)
					logger.trace("Adding listener '" + listener + "' on channel '" + topic.getTopic() + "'");
			}
			else {
				throw new IllegalArgumentException("Unknown topic type '" + topic.getClass() + "'");
			}
		}
	}



	/**
	 * Handle subscription task exception. Will attempt to restart the subscription if the Exception is a connection
	 * failure (for example, Redis was restarted).
	 * 
	 * @param ex Throwable exception
	 */
	protected void handleSubscriptionException(Throwable ex) {
		listening = false;
		subscriptionTask.closeSubscriber();
		logger.error("SubscriptionTask aborted with exception:", ex);
	}

	/**
	 * Sleep according to the specified recovery interval. Called between recovery attempts.
	 */
	protected void sleepBeforeRecoveryAttempt() {
		if (this.recoveryInterval > 0) {
			try {
				Thread.sleep(this.recoveryInterval);
			} catch (InterruptedException interEx) {
				logger.debug("Thread interrupted while sleeping the recovery interval");
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Runnable used for Redis subscription. Implemented as a dedicated class to provide as many hints as possible to the
	 * underlying thread pool.
	 * 
	 * @author Costin Leau
	 */
	private class SubscriptionTask implements SchedulingAwareRunnable {



		private volatile JedisSubscriber jedisSubscriber;
		private boolean subscriptionTaskRunning = false;
		private final Object localMonitor = new Object();
		private long subscriptionWait = TimeUnit.SECONDS.toMillis(5);

		public boolean isLongLived() {
			return true;
		}

		public void run() {
			synchronized (localMonitor) {
				subscriptionTaskRunning = true;
			}
			try {
				jedisSubscriber=new JedisSubscriber(jedisOperator.getJedis());
				synchronized (monitor) {
					monitor.notify();
				}
				 eventuallyPerformSubscription();

			} catch (Throwable t) {
				handleSubscriptionException(t);
			} finally {
				// this block is executed once the subscription thread has ended, this may or may not mean
				// the connection has been unsubscribed, depending on driver
				synchronized (localMonitor) {
					subscriptionTaskRunning = false;
					localMonitor.notify();
				}
			}
		}

		/**
		 * Performs a potentially asynchronous registration of a subscription.
		 * 
		 * @return #SubscriptionPresentCondition that can serve as a handle to check whether the subscription is ready.
		 */
		private void eventuallyPerformSubscription() {

				byte[][] channels =unwrap(channelMapping.keySet());
				
				mapTableOperator.addRegisterMapInfo(channels, jedisOperator);
				
				jedisSubscriber.subscribe(new DispatchMessageListener(), channels);

		}





		private byte[][] unwrap(Collection<ByteArrayWrapper> holders) {
			if (CollectionUtils.isEmpty(holders)) {
				return new byte[0][];
			}

			byte[][] unwrapped = new byte[holders.size()][];

			int index = 0;
			for (ByteArrayWrapper arrayHolder : holders) {
				unwrapped[index++] = arrayHolder.getArray();
			}

			return unwrapped;
		}

		void cancel() {
			if (!listening) {
				return;
			}
			listening = false;

			if (logger.isTraceEnabled()) {
				logger.trace("Cancelling Redis subscription...");
			}
			if (jedisSubscriber != null) {
				if (jedisSubscriber.isSubscribed()) {
					synchronized (localMonitor) {
						logger.trace("Unsubscribing from all channels");
						jedisSubscriber.unsubscribe();
						if (subscriptionTaskRunning) {
							try {
								localMonitor.wait(subscriptionWait);
							} catch (InterruptedException e) {
								// Stop waiting
								Thread.currentThread().interrupt();
							}
						}
						if (!subscriptionTaskRunning) {
							closeSubscriber();
						} else {
							logger.warn("Unable to close connection. Subscription task still running");
						}
					}
				}
			}
		}

		void closeSubscriber() {
			if (jedisSubscriber != null) {
				logger.trace("Closing connection");
				try {
					jedisSubscriber.close();
				} catch (Exception e) {
					logger.warn("Error closing subscription connection", e);
				}
				jedisSubscriber = null;
			}
		}


	}

	/**
	 * Actual message dispatcher/multiplexer.
	 * 
	 * @author Costin Leau
	 */
	private class DispatchMessageListener implements MessageListener {

		public void onMessage(Message message) {
			Collection<NotifyMessageListener> listeners = null;

				// do channel matching first
				listeners = channelMapping.get(new ByteArrayWrapper(message.getChannel()));

			if (!CollectionUtils.isEmpty(listeners)) {
				dispatchMessage(listeners, message);
			}
		}
	}

	private void dispatchMessage(Collection<NotifyMessageListener> listeners, final Message message) {
		final byte[] source = message.getChannel();

		for (final NotifyMessageListener messageListener : listeners) {
			taskExecutor.execute(new Runnable() {
				public void run() {
					processMessage(messageListener, message, source);
				}
			});
		}
	}

	/**
	 * Specify the interval between recovery attempts, in <b>milliseconds</b>. The default is 5000 ms, that is, 5 seconds.
	 * 
	 * @see #handleSubscriptionException
	 */
	public void setRecoveryInterval(long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	public long getMaxSubscriptionRegistrationWaitingTime() {
		return maxSubscriptionRegistrationWaitingTime;
	}

	/**
	 * Specify the max time to wait for subscription registrations, in <b>milliseconds</b>. The default is 2000ms, that
	 * is, 2 second.
	 * 
	 * @param maxSubscriptionRegistrationWaitingTime
	 * @see #DEFAULT_SUBSCRIPTION_REGISTRATION_WAIT_TIME
	 */
	public void setMaxSubscriptionRegistrationWaitingTime(long maxSubscriptionRegistrationWaitingTime) {
		this.maxSubscriptionRegistrationWaitingTime = maxSubscriptionRegistrationWaitingTime;
	}

}
