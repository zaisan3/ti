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



/**
 * Default message implementation.
 * 
 * @author Costin Leau
 */
public class DefaultMessage implements Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3415749703800684086L;
	private final byte[] channel;
	private final byte[] body;
	private String toString;

	public DefaultMessage(byte[] channel, byte[] body) {
		this.body = body;
		this.channel = channel;
	}

	public byte[] getChannel() {
		return (channel != null ? channel.clone() : null);
	}

	public byte[] getBody() {
		return (body != null ? body.clone() : null);
	}

	public String toString() {
		if (toString == null) {
			toString = new String(body);
		}
		return toString;
	}
}

