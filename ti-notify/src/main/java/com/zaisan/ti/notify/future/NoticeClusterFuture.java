package com.zaisan.ti.notify.future;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.util.CollectionUtils;

import redis.clients.jedis.Jedis;

import com.zaisan.ti.notify.Future;
import com.zaisan.ti.notify.constants.NotifyConstants;
import com.zaisan.ti.notify.future.namespace.NamespaceWrapper;
import com.zaisan.ti.notify.maptable.MapTableOperator;
import com.zaisan.ti.notify.message.NotifyFeedbackOperator;
import com.zaisan.ti.notify.message.NotifyFeedbackPack;
import com.zaisan.ti.notify.message.NotifyRegisterInfo;
import com.zaisan.ti.notify.result.NoticeResult;
import com.zaisan.ti.notify.spring.redis.JedisOperator;
import com.zaisan.ti.notify.spring.redis.RedisCallback;

public class NoticeClusterFuture implements Future<NoticeResult>{
    private JedisOperator jedisOperator;
    private MapTableOperator mapTableOperator;
    private String channelName;
    private byte[] rawChannel;
    private String messageId;
    private long msgPublishTime;
    
    private NoticeResult noticeResult = null;
    
	public NoticeClusterFuture(JedisOperator jedisOperator,MapTableOperator mapTableOperator,
			String channelName,byte[] rawChannel,String messageId,long msgPublishTime){
		this.jedisOperator=jedisOperator;
		this.mapTableOperator=mapTableOperator;
		this.channelName=channelName;
		this.rawChannel=rawChannel;
		this.messageId=messageId;
		this.msgPublishTime=msgPublishTime;
	}
	@Override
	public NoticeResult get() {
		return get(NotifyConstants.DEFAULT_MAX_WAIT_FEEDBACK,TimeUnit.SECONDS);
	}

	@Override
	public NoticeResult  get(long timeout, TimeUnit unit) {
		if(noticeResult!=null){
			return noticeResult;
		}
		//get all subscriber from redis
		Set<byte[]> allSubscriber= jedisOperator.execute(new RedisCallback<Set<byte[]>>() {
				public Set<byte[]> doInRedis(Jedis connection) {
					return connection.smembers(rawChannel);
				}
			});
		
		//get all active subscriber 
		Set<String> needWaitFeedback = new HashSet<String>();
		if(allSubscriber!=null && allSubscriber.size()>0){
			for (byte[] subscriberData : allSubscriber) {
				NotifyRegisterInfo notifyRegisterInfo = mapTableOperator.parseRegisterPack(subscriberData);
				if(notifyRegisterInfo.isAlive() && notifyRegisterInfo.registerBeforeMsgSend(msgPublishTime)){
					needWaitFeedback.add(notifyRegisterInfo.getSubcribeNodeId());
				}
			}
		}
		if(needWaitFeedback.isEmpty()){
			// no active subscriber ,not need wait feedback
			return NoticeResult.success;
		}
		Set<NotifyFeedbackPack> receiveFeedback=new HashSet<NotifyFeedbackPack>();
		long deadline=System.currentTimeMillis()+unit.toMillis(timeout);
		byte [] messageIdQueue = NamespaceWrapper.wrapperWithNamespace(messageId).getBytes();
		do {
			byte[] feedbackPack= jedisOperator.execute(new RedisCallback<byte[]>() {
				public byte[] doInRedis(Jedis connection) {
					List<byte[]> result = connection.blpop((int)timeout, messageIdQueue); 
					return (CollectionUtils.isEmpty(result) ? null : result.get(1));
				}
			});
			if(feedbackPack!=null && feedbackPack.length>0){
				receiveFeedback.add(NotifyFeedbackOperator.deserializeFeedbackPack(feedbackPack));				
			}
		}while(receiveFeedback.size()<needWaitFeedback.size() && System.currentTimeMillis()<deadline);
		
		 noticeResult=getNoticeResult(receiveFeedback, needWaitFeedback);
		 return noticeResult;
	}

	private NoticeResult getNoticeResult(Set<NotifyFeedbackPack> receiveFeedback,Set<String> needWaitFeedback){
		Set<String> notFeedback = new HashSet<String>();
		Set<NotifyFeedbackPack> handlerSuccess = new HashSet<NotifyFeedbackPack>();
		Set<NotifyFeedbackPack> handlerError = new HashSet<NotifyFeedbackPack>();
		for (String clientId : needWaitFeedback) {
			boolean feedback=false;
			for (NotifyFeedbackPack notifyFeedbackPack : receiveFeedback) {
				
				if(notifyFeedbackPack.getClientId().equals(clientId)){
					if(notifyFeedbackPack.isHandlerSuccess()){
						handlerSuccess.add(notifyFeedbackPack);
					}else{
						handlerError.add(notifyFeedbackPack);
					}
					feedback=true;
				}
			}
			if(!feedback){
				notFeedback.add(clientId);
			}
		}
		
		if(handlerSuccess.size()==needWaitFeedback.size()){
			StringBuilder showMsg = new StringBuilder();
			showMsg.append("系统向频道").append(channelName).append("发送消息后,所有监听者均成功处理，监听者反馈如下：<br>");
			for (NotifyFeedbackPack notifyFeedbackPack : handlerSuccess) {
				showMsg.append("客户端代号：").append(notifyFeedbackPack.getClientId()).append("<br>");
				showMsg.append("客户端信息：").append(notifyFeedbackPack.getMsg()).append("<br>");
			}
			return new NoticeResult() {
				@Override
				public boolean isAllClientSuccess() {
					return true;
				}
				@Override
				public String getResultString() {
					return showMsg.toString();
				}
			};
		}
		
		StringBuilder showMsg = new StringBuilder();
		showMsg.append("系统向频道").append(channelName).append("发送消息时发生异常，情况如下:<br>");
		if(notFeedback.size()>0){
			showMsg.append("以下客户端长时间未反馈消息的消费情况：").append(notFeedback.toString()).append("<br>");
		}
		if(handlerError.size()>0){
			showMsg.append("以下客户端反馈消费消息时发生异常：").append("<br>");
			for (NotifyFeedbackPack notifyFeedbackPack : handlerError) {
				showMsg.append("客户端代号：").append(notifyFeedbackPack.getClientId()).append("<br>");
				showMsg.append("客户端异常信息：").append(notifyFeedbackPack.getMsg()).append("<br>");
			}
		}
		return new NoticeResult() {
			@Override
			public boolean isAllClientSuccess() {
				return false;
			}
			@Override
			public String getResultString() {
				return showMsg.toString();
			}
		};
	}
}
