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
package com.zaisan.ti.notify.util;

import java.util.Arrays;


public class ByteArrayWrapper {

	private final byte[] array;
	private final int hashCode;

	public ByteArrayWrapper(byte[] array) {
		this.array = array;
		this.hashCode = Arrays.hashCode(array);
	}

	public boolean equals(Object obj) {
		if (obj instanceof ByteArrayWrapper) {
			return Arrays.equals(array, ((ByteArrayWrapper) obj).array);
		}

		return false;
	}

	public int hashCode() {
		return hashCode;
	}

	/**
	 * Returns the array.
	 * 
	 * @return Returns the array
	 */
	public byte[] getArray() {
		return array;
	}
}
