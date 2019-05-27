package com.zaisan.ti.notify.spring.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import com.zaisan.ti.notify.ChannelTopic;
import com.zaisan.ti.notify.Topic;
import com.zaisan.ti.notify.future.namespace.NamespaceWrapper;

public class RedisListenerContainerParser extends AbstractSimpleBeanDefinitionParser{

	protected Class<RedisMessageListenerContainer> getBeanClass(Element element) {
		return RedisMessageListenerContainer.class;
	}

	@SuppressWarnings("unchecked")
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// parse attributes (but replace the value assignment with references)
		NamedNodeMap attributes = element.getAttributes();

		for (int x = 0; x < attributes.getLength(); x++) {
			Attr attribute = (Attr) attributes.item(x);
			if (isEligibleAttribute(attribute, parserContext)) {
				String propertyName = extractPropertyName(attribute.getLocalName());
				Assert.state(StringUtils.hasText(propertyName),
						"Illegal property name returned from 'extractPropertyName(String)': cannot be null or empty.");
				if("namespace".equals(propertyName)){
					 NamespaceWrapper.initGlobalNameSpace(attribute.getValue());
				}else{					
					builder.addPropertyReference(propertyName, attribute.getValue());
				}
			}
		}

		String phase = element.getAttribute("phase");
		if (StringUtils.hasText(phase)) {
			builder.addPropertyValue("phase", phase);
		}

		postProcess(builder, element);

		// parse nested listeners
		List<Element> listDefs = DomUtils.getChildElementsByTagName(element, "listener");

		if (!listDefs.isEmpty()) {
			ManagedMap<BeanDefinition, Collection<? extends BeanDefinition>> listeners = new ManagedMap<BeanDefinition, Collection<? extends BeanDefinition>>(
					listDefs.size());
			for (Element listElement : listDefs) {
				Object[] listenerDefinition = parseListener(listElement);
				listeners.put((BeanDefinition) listenerDefinition[0],
						(Collection<? extends BeanDefinition>) listenerDefinition[1]);
			}

			builder.addPropertyValue("messageListeners", listeners);
		}
	}

	protected boolean isEligibleAttribute(String attributeName) {
		return (!"phase".equals(attributeName));
	}

	/**
	 * Parses a listener definition. Returns the listener bean reference definition (as the array first entry) and its
	 * associated topics (also as bean definitions).
	 * 
	 * @param element
	 * @return
	 */
	private Object[] parseListener(Element element) {
		Object[] ret = new Object[2];

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MessageListenerAdapter.class);
		builder.addConstructorArgReference(element.getAttribute("ref"));

		String method = element.getAttribute("method");
		if (StringUtils.hasText(method)) {
			builder.addPropertyValue("defaultListenerMethod", method);
		}

		String serializer = element.getAttribute("serializer");
		if (StringUtils.hasText(serializer)) {
			builder.addPropertyReference("serializer", serializer);
		}

		// assemble topics
		Collection<Topic> topics = new ArrayList<Topic>();

		// get topic
		String xTopics = element.getAttribute("topic");
		if (StringUtils.hasText(xTopics)) {
			String[] array = StringUtils.delimitedListToStringArray(xTopics, " ");

			for (String string : array) {
				String topicName = NamespaceWrapper.wrapperWithNamespace(string);
				topics.add(new ChannelTopic(topicName));
			}
		}
		ret[0] = builder.getBeanDefinition();
		ret[1] = topics;

		return ret;
	}

	protected boolean shouldGenerateId() {
		return true;
	}
}
