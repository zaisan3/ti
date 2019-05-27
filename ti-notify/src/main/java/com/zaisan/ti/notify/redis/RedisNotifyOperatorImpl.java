package com.zaisan.ti.notify.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.StringUtils;

import redis.clients.jedis.Jedis;

import com.zaisan.ti.notify.Future;
import com.zaisan.ti.notify.NotifyOperator;
import com.zaisan.ti.notify.future.NoticeClusterFuture;
import com.zaisan.ti.notify.future.namespace.NamespaceWrapper;
import com.zaisan.ti.notify.maptable.MapTableOperator;
import com.zaisan.ti.notify.message.NotifyMessage;
import com.zaisan.ti.notify.message.NotifyMessageOperator;
import com.zaisan.ti.notify.message.NotifyRegisterInfo;
import com.zaisan.ti.notify.result.NoticeResult;
import com.zaisan.ti.notify.spring.redis.DefaultJedisOperator;
import com.zaisan.ti.notify.spring.redis.JedisOperator;
import com.zaisan.ti.notify.spring.redis.RedisCallback;
import com.zaisan.ti.notify.util.NotifyUtils;

public class RedisNotifyOperatorImpl implements NotifyOperator{
	
    private JedisOperator jedisOperator;
    
    private NotifyMessageOperator  messageOperator = new NotifyMessageOperator();
    
    private MapTableOperator mapTableOperator= new MapTableOperator();
	//定时线程池，单线程线程池-垃圾清理
	private volatile ScheduledExecutorService scheduler = null;
	
	private ConcurrentHashMap<String, byte[]> historyChannelMap = new ConcurrentHashMap<String,byte[]>();
	@Override
	public Future<NoticeResult> noticeCluster(String channelName) {
		return noticeCluster(channelName, String.valueOf(System.currentTimeMillis()));
	}

	@Override
	public Future<NoticeResult> noticeCluster(String channelName, String data) {
		channelName = NamespaceWrapper.wrapperWithNamespace(channelName);
		final byte[] rawChannel = messageOperator.serializeChannelName(channelName);
		historyChannelMap.putIfAbsent(channelName, rawChannel);
		startCleanTaskIfNecessary();
		
		NotifyMessage notifyMessage =new NotifyMessage(NotifyUtils.genernateMessageId(), channelName, data);
		final String notifyMessageId = notifyMessage.getMessageId();
		final byte[] rawMessage = messageOperator.genernateMessagePack(notifyMessage);
		final AtomicLong publishTime = new AtomicLong();
		jedisOperator.execute(new RedisCallback<Long>() {
			public Long doInRedis(Jedis connection) {
				Long count=connection.publish(rawChannel, rawMessage);
				publishTime.set(System.currentTimeMillis());
				return count;
			}
		});
		return new NoticeClusterFuture(jedisOperator, mapTableOperator, channelName,rawChannel,
				notifyMessageId,publishTime.get());
	}
	
	public void setNamespace(String namespace) {
		if(!StringUtils.isEmpty(namespace)){
			NamespaceWrapper.initGlobalNameSpace(namespace);			
		}
	}
	
	
	private void startCleanTaskIfNecessary() {
		if(scheduler!=null)return;
		synchronized (this) {
			if(scheduler!=null)return;
			 scheduler = Executors.newSingleThreadScheduledExecutor();
			 scheduler.scheduleWithFixedDelay(new CleanInactiveChannelSubscriberTask(), 5, 5, TimeUnit.MINUTES);
		}
		
	}


	/**
	 * 一个channel上的订阅者 经常因为重启，停机升级而下线，造成 channel上的订阅者存在僵尸情况，
	 * 这里定义清理任务，帮助清理僵尸节点
	 * @author jiajia.sang
	 *
	 */
	private class CleanInactiveChannelSubscriberTask implements Runnable{
		@Override
		public void run() {
			if(historyChannelMap.isEmpty())return;
			for (String channelName : historyChannelMap.keySet()) {
				byte [] rawChannelByte = historyChannelMap.get(channelName);
				//get all subscriber from redis
				Set<byte[]> allSubscriber= jedisOperator.execute(new RedisCallback<Set<byte[]>>() {
						public Set<byte[]> doInRedis(Jedis connection) {
							return connection.smembers(rawChannelByte);
						}
					});
				if(allSubscriber!=null && allSubscriber.size()>0){
					List<byte[]> zombileNode = new ArrayList<byte[]>();
					for (byte[] subscriberData : allSubscriber) {
						NotifyRegisterInfo notifyRegisterInfo = mapTableOperator.parseRegisterPack(subscriberData);
						if(notifyRegisterInfo.isZombieNode()){
							zombileNode.add(subscriberData);
						}
					}
					if(zombileNode.size()>0){
						jedisOperator.execute(new RedisCallback<Long>() {
							public Long doInRedis(Jedis connection) {
								byte[][] deleteSubcribers = new byte[zombileNode.size()][];
								return connection.srem(rawChannelByte, zombileNode.toArray(deleteSubcribers));
							}
						});
					}
				}
			}
			
		}
		
	}
	
	public void setJedisProvider(JedisProvider jedisProvider) {
		jedisOperator= new DefaultJedisOperator();
		jedisOperator.setJedisProvider(jedisProvider);
	}
}
