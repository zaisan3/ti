/**
 * Copyright (c) 2017 Baozun All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Baozun.
 * You shall not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Baozun.
 *
 * BAOZUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. BAOZUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */
package com.zaisan.ti.notify.spring.redis;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

public class JedisSubscriber {

	private Jedis jedis;
	
	private boolean isSubscribed;

	
	private BinaryJedisPubSub jedisPubSub;
			
	

	public JedisSubscriber(Jedis jedis) {
		this.jedis = jedis;
	}
	
	public  void subscribe(MessageListener listener, byte[]... channels){
		if (isSubscribed) {
			throw new RuntimeException(
					"Connection already subscribed; use the connection Subscription to cancel or add new channels");
		}


			jedisPubSub = new JedisMessageListener(listener);

			isSubscribed = true;
			
			/*subscription = new JedisSubscription(listener, jedisPubSub, channels, null);*/
			jedis.subscribe(jedisPubSub, channels);

			
	}
	
	
	public boolean isSubscribed(){
		return isSubscribed;
	}
	
	public void unsubscribe(){
		if(!isSubscribed())return;
		
		jedisPubSub.unsubscribe();
	}
	
	public void close(){
		unsubscribe();
		if(jedis!=null){
			jedis.close();			
		}
	}
}
