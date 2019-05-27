package com.zaisan.ti.notify.message;

import com.zaisan.ti.notify.constants.NotifyConstants;
import com.zaisan.ti.notify.spring.redis.RedisSerializer;
import com.zaisan.ti.notify.spring.redis.StringRedisSerializer;
import com.zaisan.ti.notify.util.NotifyUtils;

public class NotifyFeedbackOperator {
	public static final String success_code="1";
	public static final String error_code="2";
	public static final String success_ack_code="ok";
	
	private static RedisSerializer<String> stringSerializer = new StringRedisSerializer();
	public static byte[] successMsg(){
		StringBuilder sb = new StringBuilder();
		sb.append(NotifyUtils.getCurNodeIdStr());
		sb.append(NotifyConstants.SPLITCHARS);
		sb.append(success_code);
		sb.append(NotifyConstants.SPLITCHARS);
		sb.append(success_ack_code);
		return stringSerializer.serialize(sb.toString());
	}
	
	public static byte[] errorMsg(String errorMsg){
		StringBuilder sb = new StringBuilder();
		sb.append(NotifyUtils.getCurNodeIdStr());
		sb.append(NotifyConstants.SPLITCHARS);
		sb.append(error_code);
		sb.append(NotifyConstants.SPLITCHARS);
		sb.append(errorMsg);
		return stringSerializer.serialize(sb.toString());
	}
	
	public static NotifyFeedbackPack deserializeFeedbackPack(byte[] packData){
		String packStr = stringSerializer.deserialize(packData);
		String [] feedbackAttrs = packStr.split(NotifyConstants.SPLITCHARS);
		NotifyFeedbackPack nfp = new NotifyFeedbackPack();
		nfp.setClientId(feedbackAttrs[0]);
		nfp.setHandlerSuccess(success_code.equals(feedbackAttrs[1]));
		if(feedbackAttrs.length>=3){
			nfp.setMsg(feedbackAttrs[2]);			
		}
		return nfp;
	}
	
}
