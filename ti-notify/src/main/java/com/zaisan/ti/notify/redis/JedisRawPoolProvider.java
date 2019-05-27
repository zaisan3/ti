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

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

/**
 * 若使用notify的项目中 仅使用jedis原生jar包来操作redis缓存，可以配置为由 bean: Pool<Jedis>
 * 来提供jedis数据源
 * @author jiajia.sang
 *
 */
public class JedisRawPoolProvider implements JedisProvider{

	@Autowired
	private Pool<Jedis> pool;
	@Override
	public Jedis getJedis() {
		return pool.getResource();
	}

}
