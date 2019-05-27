package com.zaisan.ti.notify.spring.redis;

import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * {@link NamespaceHandler} for Spring Data Redis namespace.
 * 
 * @author Costin Leau
 */
class RedisNamespaceHandler extends NamespaceHandlerSupport {

	public void init() {
		registerBeanDefinitionParser("listener-container", new RedisListenerContainerParser());
	}
}