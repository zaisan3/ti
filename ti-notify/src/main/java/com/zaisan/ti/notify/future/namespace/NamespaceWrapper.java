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
package com.zaisan.ti.notify.future.namespace;

import org.springframework.util.StringUtils;


public class NamespaceWrapper {

	public static final String NAMES_SPACE_DEFAULT="ti-notify";
	
	public static String namespace = NAMES_SPACE_DEFAULT;
	
	public static String wrapperWithNamespace(String originValue){
		return namespace.concat("-").concat(originValue);
	}
	
	public static void initGlobalNameSpace(String namespace){
		if(!StringUtils.isEmpty(namespace)){
			NamespaceWrapper.namespace=namespace;			
		}
	}
}
