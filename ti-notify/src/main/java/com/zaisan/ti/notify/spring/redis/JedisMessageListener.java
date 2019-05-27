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

import org.springframework.util.Assert;

import redis.clients.jedis.BinaryJedisPubSub;

class JedisMessageListener extends BinaryJedisPubSub {

	private final MessageListener listener;

	JedisMessageListener(MessageListener listener) {
		Assert.notNull(listener, "message listener is required");
		this.listener = listener;
	}

	public void onMessage(byte[] channel, byte[] message) {
		listener.onMessage(new DefaultMessage(channel, message));
	}

	public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
		// no-op
	}

	public void onPSubscribe(byte[] pattern, int subscribedChannels) {
		// no-op
	}

	public void onPUnsubscribe(byte[] pattern, int subscribedChannels) {
		// no-op
	}

	public void onSubscribe(byte[] channel, int subscribedChannels) {
		// no-op
	}

	public void onUnsubscribe(byte[] channel, int subscribedChannels) {
		// no-op
	}
}

