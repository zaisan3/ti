package com.zaisan.ti.notify.message;


import com.zaisan.ti.notify.constants.NotifyConstants;
import com.zaisan.ti.notify.spring.redis.Message;
import com.zaisan.ti.notify.spring.redis.StringRedisSerializer;

public class NotifyMessageOperator {
	private StringRedisSerializer stringSerializer = new StringRedisSerializer();
	
	public  byte[] serializeChannelName(String channelName){
		return stringSerializer.serialize(channelName);
	}
	
	public byte[] genernateMessagePack(NotifyMessage notifyMessage){
		return notifyMessage.getMessageId().concat(NotifyConstants.SPLITCHARS).concat(notifyMessage.getContent()).getBytes();
	}
	
	public NotifyMessage deserializeMessage(Message message,byte[] pattern){
		String [] messageIdAndContent=stringSerializer.deserialize(message.getBody()).split(NotifyConstants.SPLITCHARS);
		String messageId = messageIdAndContent[0];
		String messageContent = messageIdAndContent[1];
		String channelName = stringSerializer.deserialize(pattern);
		return new NotifyMessage(messageId, channelName, messageContent);
	}
}
