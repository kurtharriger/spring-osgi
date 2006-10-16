/*
 * Copyright 2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.osgi.config;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;
import org.w3c.dom.Element;

/**
 * @author Hal Hildebrand
 *         Date: May 19, 2006
 *         Time: 4:40:50 PM
 */
public class ServiceReferenceBeanDefinitionParser implements BeanDefinitionParser {
    private static final String NAME_ATTRIBUTE = "name";
    private static final String BEAN_NAME_ATTRIBUTE = "beanName";
    private static final String FILTER_ATTRIBUTE = "filter";
    private static final String MAX_RETRIES_ATTRIBUTE = "maxRetries";
    private static final String MILLIS_BETWEEN_RETRIES_ATTRIBUTE = "millisBetweenRetries";
    private static final String RETRY_ON_UNREGISTERED_SERVICE_ATTRIBUTE = "retryOnUnregisteredService";
    private static final String SERVICE_TYPE_ATTRIBUTE = "serviceType";

    private static final String BEAN_NAME_PROPERTY = "beanName";
    private static final String FILTER_PROPERTY = "filter";
    private static final String MAX_RETRIES_PROPERTY = "maxRetries";
    private static final String MILLIS_BETWEEN_RETRIES_PROPERTY = "millisBetweenRetries";
    private static final String RETRY_ON_UNREGISTERED_SERVICE_PROPERTY = "retryOnUnregisteredService";
    private static final String SERVICE_TYPE_PROPERY = "serviceType";

    /* (non-Javadoc)
      * @see org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
      */
    public BeanDefinition parse(Element element, ParserContext parserContext) { 
        BeanDefinitionRegistry registry = parserContext.getRegistry();
        String referenceName = element.getAttribute(NAME_ATTRIBUTE);
        BeanDefinition beanDef = createBeanDefinition(element);
        registry.registerBeanDefinition(referenceName, beanDef);
        return beanDef;
    }

    private BeanDefinition createBeanDefinition(Element element) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(OsgiServiceProxyFactoryBean.class);
        MutablePropertyValues props = new MutablePropertyValues();
        props.addPropertyValue(SERVICE_TYPE_ATTRIBUTE, element.getAttribute(SERVICE_TYPE_PROPERY)); 
        if (element.hasAttribute(BEAN_NAME_ATTRIBUTE)) {
            props.addPropertyValue(BEAN_NAME_PROPERTY, element.getAttribute(BEAN_NAME_ATTRIBUTE));
        }
        if (element.hasAttribute(FILTER_ATTRIBUTE)) {
            props.addPropertyValue(FILTER_PROPERTY, element.getAttribute(FILTER_ATTRIBUTE));
        }
        if (element.hasAttribute(MAX_RETRIES_ATTRIBUTE)) {
            props.addPropertyValue(MAX_RETRIES_PROPERTY, element.getAttribute(MAX_RETRIES_ATTRIBUTE));
        }
        if (element.hasAttribute(MILLIS_BETWEEN_RETRIES_ATTRIBUTE)) {
            props.addPropertyValue(MILLIS_BETWEEN_RETRIES_PROPERTY, element.getAttribute(MILLIS_BETWEEN_RETRIES_ATTRIBUTE));
        }
        if (element.hasAttribute(RETRY_ON_UNREGISTERED_SERVICE_ATTRIBUTE)) {
            props.addPropertyValue(RETRY_ON_UNREGISTERED_SERVICE_PROPERTY, element.getAttribute(RETRY_ON_UNREGISTERED_SERVICE_ATTRIBUTE));
        }
        beanDefinition.setPropertyValues(props);
        return beanDefinition;
    }
}
