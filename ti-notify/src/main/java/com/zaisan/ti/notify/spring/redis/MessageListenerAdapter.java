package com.zaisan.ti.notify.spring.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.util.StringUtils;

import redis.clients.jedis.Jedis;

import com.zaisan.ti.notify.future.namespace.NamespaceWrapper;
import com.zaisan.ti.notify.message.NotifyFeedbackOperator;
import com.zaisan.ti.notify.message.NotifyMessage;
import com.zaisan.ti.notify.message.NotifyMessageOperator;

public class MessageListenerAdapter implements InitializingBean, NotifyMessageListener{


	private class MethodInvoker {

		private final Object delegate;
		private String methodName;
		private Set<Method> methods;
		private boolean lenient;

		MethodInvoker(Object delegate, final String methodName) {

			this.delegate = delegate;
			this.methodName = methodName;
			this.lenient = delegate instanceof MessageListener;
			this.methods = new HashSet<Method>();

			final Class<?> c = delegate.getClass();

			ReflectionUtils.doWithMethods(c, new MethodCallback() {

				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					ReflectionUtils.makeAccessible(method);
					methods.add(method);
				}

			}, new MostSpecificMethodFilter(methodName, c));

			Assert.isTrue(lenient || !methods.isEmpty(), "Cannot find a suitable method named [" + c.getName() + "#"
					+ methodName + "] - is the method public and has the proper arguments?");
		}

		void invoke(Object[] arguments) throws InvocationTargetException, IllegalAccessException {

			Object[] message = new Object[] { arguments[0] };

			for (Method m : methods) {

				Class<?>[] types = m.getParameterTypes();
				Object[] args = //
				types.length == 2 //
						&& types[0].isInstance(arguments[0]) //
						&& types[1].isInstance(arguments[1]) ? arguments : message;

				if (!types[0].isInstance(args[0])) {
					continue;
				}

				m.invoke(delegate, args);

				return;
			}
		}

		/**
		 * Returns the current methodName.
		 * 
		 * @return the methodName
		 */
		public String getMethodName() {
			return methodName;
		}
	}
	
	/**
	 * Out-of-the-box value for the default listener method: "handleMessage".
	 */
	public static final String ORIGINAL_DEFAULT_LISTENER_METHOD = "handleMessage";

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private volatile Object delegate;

	private volatile MethodInvoker invoker;

	private String defaultListenerMethod = ORIGINAL_DEFAULT_LISTENER_METHOD;

	private RedisSerializer<?> serializer;

	private RedisSerializer<String> stringSerializer;
	
	private NotifyMessageOperator  messageOperator = new NotifyMessageOperator();

	/**
	 * Create a new {@link MessageListenerAdapter} with default settings.
	 */
	public MessageListenerAdapter() {
		initDefaultStrategies();
		this.delegate = this;
	}

	/**
	 * Create a new {@link MessageListenerAdapter} for the given delegate.
	 * 
	 * @param delegate the delegate object
	 */
	public MessageListenerAdapter(Object delegate) {
		initDefaultStrategies();
		setDelegate(delegate);
	}

	/**
	 * Create a new {@link MessageListenerAdapter} for the given delegate.
	 * 
	 * @param delegate the delegate object
	 * @param defaultListenerMethod method to call when a message comes
	 * @see #getListenerMethodName
	 */
	public MessageListenerAdapter(Object delegate, String defaultListenerMethod) {
		this(delegate);
		setDefaultListenerMethod(defaultListenerMethod);
	}

	/**
	 * Set a target object to delegate message listening to. Specified listener methods have to be present on this target
	 * object.
	 * <p>
	 * If no explicit delegate object has been specified, listener methods are expected to present on this adapter
	 * instance, that is, on a custom subclass of this adapter, defining listener methods.
	 * 
	 * @param delegate delegate object
	 */
	public void setDelegate(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * Returns the target object to delegate message listening to.
	 * 
	 * @return message listening delegation
	 */
	public Object getDelegate() {
		return this.delegate;
	}

	/**
	 * Specify the name of the default listener method to delegate to, for the case where no specific listener method has
	 * been determined. Out-of-the-box value is {@link #ORIGINAL_DEFAULT_LISTENER_METHOD "handleMessage"}.
	 * 
	 * @see #getListenerMethodName
	 */
	public void setDefaultListenerMethod(String defaultListenerMethod) {
		this.defaultListenerMethod = defaultListenerMethod;
	}

	/**
	 * Return the name of the default listener method to delegate to.
	 */
	protected String getDefaultListenerMethod() {
		return this.defaultListenerMethod;
	}

	/**
	 * Set the serializer that will convert incoming raw Redis messages to listener method arguments.
	 * <p>
	 * The default converter is a {@link StringRedisSerializer}.
	 * 
	 * @param serializer
	 */
	public void setSerializer(RedisSerializer<?> serializer) {
		this.serializer = serializer;
	}

	/**
	 * Sets the serializer used for converting the channel/pattern to a String.
	 * <p>
	 * The default converter is a {@link StringRedisSerializer}.
	 * 
	 * @param serializer
	 */
	public void setStringSerializer(RedisSerializer<String> serializer) {
		this.stringSerializer = serializer;
	}

	public void afterPropertiesSet() {
		String methodName = getDefaultListenerMethod();

		if (!StringUtils.hasText(methodName)) {
			throw new IllegalArgumentException("No default listener method specified: "
					+ "Either specify a non-null value for the 'defaultListenerMethod' property or "
					+ "override the 'getListenerMethodName' method.");
		}

		invoker = new MethodInvoker(delegate, methodName);
	}

	/**
	 * Standard Redis {@link MessageListener} entry point.
	 * <p>
	 * Delegates the message to the target listener method, with appropriate conversion of the message argument. In case
	 * of an exception, the {@link #handleListenerException(Throwable)} method will be invoked.
	 * 
	 * @param message the incoming Redis message
	 * @see #handleListenerException
	 */

	public void onMessage(Message message, byte[] pattern,Jedis jedis) {
		NotifyMessage notifyMessage = messageOperator.deserializeMessage(message, pattern);
		byte[] messageIdQueue = NamespaceWrapper.wrapperWithNamespace(notifyMessage.getMessageId()).getBytes();
		try {
			// Check whether the delegate is a MessageListener impl itself.
			// In that case, the adapter will simply act as a pass-through.
			if (delegate != this) {
				if (delegate instanceof MessageListener) {
					((MessageListener) delegate).onMessage(message);
					return;
				}
			}

			// Invoke the handler method with appropriate arguments.
			Object[] listenerArguments = new Object[] { notifyMessage.getContent(), notifyMessage.getChannelName() };

			invoker.invoke(listenerArguments);
			jedis.rpush(messageIdQueue, NotifyFeedbackOperator.successMsg());
		} catch (Throwable th) {
			jedis.rpush(messageIdQueue, NotifyFeedbackOperator.errorMsg(th.getMessage()));
			handleListenerException(th);
		}
	}

	/**
	 * Initialize the default implementations for the adapter's strategies.
	 * 
	 * @see #setSerializer(RedisSerializer)
	 * @see JdkSerializationRedisSerializer
	 */
	protected void initDefaultStrategies() {
		RedisSerializer<String> serializer = (RedisSerializer<String>) new StringRedisSerializer();
		setSerializer(serializer);
		setStringSerializer(serializer);
	}

	/**
	 * Handle the given exception that arose during listener execution. The default implementation logs the exception at
	 * error level.
	 * 
	 * @param ex the exception to handle
	 */
	protected void handleListenerException(Throwable ex) {
		logger.error("Listener execution failed", ex);
	}

	/**
	 * Extract the message body from the given Redis message.
	 * 
	 * @param message the Redis <code>Message</code>
	 * @return the content of the message, to be passed into the listener method as argument
	 */
	protected Object extractMessage(Message message) {
		if (serializer != null) {
			return serializer.deserialize(message.getBody());
		}
		return message.getBody();
	}

	/**
	 * Determine the name of the listener method that is supposed to handle the given message.
	 * <p>
	 * The default implementation simply returns the configured default listener method, if any.
	 * 
	 * @param originalMessage the Redis request message
	 * @param extractedMessage the converted Redis request message, to be passed into the listener method as argument
	 * @return the name of the listener method (never <code>null</code>)
	 * @see #setDefaultListenerMethod
	 */
	protected String getListenerMethodName(Message originalMessage, Object extractedMessage) {
		return getDefaultListenerMethod();
	}


	/**
	 * @since 1.4
	 */
	static final class MostSpecificMethodFilter implements MethodFilter {

		private final String methodName;
		private final Class<?> c;

		MostSpecificMethodFilter(String methodName, Class<?> c) {

			this.methodName = methodName;
			this.c = c;
		}

		public boolean matches(Method method) {

			if (Modifier.isPublic(method.getModifiers()) //
					&& methodName.equals(method.getName()) //
					&& method.equals(ClassUtils.getMostSpecificMethod(method, c))) {

				// check out the argument numbers
				Class<?>[] parameterTypes = method.getParameterTypes();

				return ((parameterTypes.length == 2 && String.class.equals(parameterTypes[1])) || parameterTypes.length == 1);
			}

			return false;
		}
	}

}
