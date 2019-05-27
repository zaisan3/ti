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
package com.zaisan.ti.notify.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import redis.clients.jedis.Jedis;

/**
 * 若使用notify的项目中 使用了spring-data-redis，可以配置为由 bean: jedisConnectionFactory
 * 来提供jedis数据源
 * @author jiajia.sang
 *
 */
public class JedisSpringConnectionFactoryProvider implements JedisProvider{

	@Autowired
	private JedisConnectionFactory jedisConnectionFactory;
	@Override
	public Jedis getJedis() {
		return (Jedis)jedisConnectionFactory.getConnection().getNativeConnection();
	}

}
