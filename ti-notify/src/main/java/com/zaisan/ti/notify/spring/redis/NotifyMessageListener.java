package com.zaisan.ti.notify.spring.redis;

import redis.clients.jedis.Jedis;


public interface NotifyMessageListener {
	/**
	 * Callback for processing received objects through Redis.
	 * 
	 * @param message message
	 * @param pattern pattern matching the channel (if specified) - can be null
	 */
	void onMessage(Message message, byte[] pattern,Jedis jedis);
}
