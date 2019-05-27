package com.zaisan.ti.notify.maptable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import com.zaisan.ti.notify.constants.NotifyConstants;
import com.zaisan.ti.notify.message.NotifyRegisterInfo;
import com.zaisan.ti.notify.spring.redis.JedisOperator;
import com.zaisan.ti.notify.spring.redis.RedisCallback;
import com.zaisan.ti.notify.spring.redis.RedisSerializer;
import com.zaisan.ti.notify.spring.redis.StringRedisSerializer;
import com.zaisan.ti.notify.util.NotifyUtils;

/**
 * 订阅关系维护，心跳等操作类
 * @author jiajia.sang
 *
 */
public class MapTableOperator {
	public static final Log logger = LogFactory.getLog(MapTableOperator.class);
	
	private Map<byte[],byte[]> lasterRegisterInfo = new HashMap<byte[],byte[]>();
	private Map<byte[],Long> registerTimeMap = new HashMap<byte[],Long>();
	private RedisSerializer<String> stringSerializer = new StringRedisSerializer();
	//定时线程池，单线程线程池
	private  ScheduledExecutorService scheduler = null;
	public  void addRegisterMapInfo(final byte[][] channels,final JedisOperator jedisOperator){
		if(channels!=null && channels.length>0){
			
			jedisOperator.execute(new RedisCallback<Long>() {
				@Override
				public Long doInRedis(Jedis jedis) {
					for (byte[] channel : channels) {
						registerTimeMap.put(channel, System.currentTimeMillis());
						lasterRegisterInfo.put(channel, genernateRegisterPack(registerTimeMap.get(channel)));
						jedis.sadd(channel, lasterRegisterInfo.get(channel));					
					}
					return null;
				}
			});
			if(scheduler==null){
				scheduler = Executors.newSingleThreadScheduledExecutor();
			}
			scheduler.scheduleAtFixedRate(new Runnable(){
				@Override
				public void run() {
					try {
						jedisOperator.execute(new RedisCallback<Long>() {
							@Override
							public Long doInRedis(Jedis heartbeatConnection) {
								Transaction transaction =heartbeatConnection.multi();
								for (byte[] channel : channels) {
									byte[] lastReisterValue=lasterRegisterInfo.put(channel, genernateRegisterPack(registerTimeMap.get(channel)));
									transaction.sadd(channel, lasterRegisterInfo.get(channel));	
									if(lastReisterValue!=null){
										transaction.srem(channel, lastReisterValue);
									}
								}
								transaction.exec();
								return null;
							}
						});
					} catch (Exception e) {
						logger.error("notify-heart-beat-task-error",e);
					}
				}
				
			}, NotifyConstants.HEART_BEAT_PERIOD, NotifyConstants.HEART_BEAT_PERIOD,TimeUnit.SECONDS);
			
			
			
			Runtime.getRuntime().addShutdownHook(new Thread(){
				@Override
				public void run() {
					
					logger.info("开始关闭通知模块的心跳任务");
					scheduler.shutdownNow();
					if(!scheduler.isTerminated()){
						logger.info("试图关闭通知模块的心跳任务后，线程池依然有活跃线程，再等待5s");
						try {
							scheduler.awaitTermination(5, TimeUnit.SECONDS);
						} catch (InterruptedException e) {
							logger.error("关闭通知模块的心跳任务出错",e);
						}
						if(!scheduler.isTerminated()){
							logger.error("试图关闭通知模块的心跳任务后，线程池依然有活跃线程，再等待2s,2s已过，线程池依然无法关闭，不管了");
						}else{
							logger.info("通知模块的心跳任务已经成功关闭");
						}
					}else{
						logger.info("通知模块的心跳任务已经成功关闭");
					}
					
					logger.info("开始去除订阅的频道");
					
					jedisOperator.execute(new RedisCallback<Long>() {
						@Override
						public Long doInRedis(Jedis jedis) {
							for (byte[] channel : channels) {
								jedis.srem(channel, lasterRegisterInfo.get(channel));
							}
							return null;
						}
					});
					lasterRegisterInfo.clear();
					lasterRegisterInfo=null;
				}
			});
			
		}
	}
	
	public  byte[] genernateRegisterPack(long subcribeTime){
		StringBuilder sb = new StringBuilder();
		sb.append(NotifyUtils.getCurNodeIdStr())
		.append(NotifyConstants.SPLITCHARS).append(subcribeTime).append(NotifyConstants.SPLITCHARS)
		.append(System.currentTimeMillis()+NotifyConstants.HEART_BEAT_PERIOD*2*1000);
		return stringSerializer.serialize(sb.toString());
	}
	
	public  NotifyRegisterInfo parseRegisterPack(byte[] registerPack){
		String registerPackStr = stringSerializer.deserialize(registerPack);
		String [] registerPackAttributes = registerPackStr.split(NotifyConstants.SPLITCHARS);
		NotifyRegisterInfo notifyRegisterInfo = new NotifyRegisterInfo();
		notifyRegisterInfo.setSubcribeNodeId(registerPackAttributes[0]);
		notifyRegisterInfo.setSubcribeTime(Long.valueOf(registerPackAttributes[1]));
		notifyRegisterInfo.setSurvivalTime(Long.valueOf(registerPackAttributes[2]));
		return notifyRegisterInfo;
	}
}
