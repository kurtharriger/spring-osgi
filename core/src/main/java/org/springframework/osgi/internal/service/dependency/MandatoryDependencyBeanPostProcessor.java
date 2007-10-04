/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.internal.service.dependency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.internal.service.ServiceExporter;
import org.springframework.util.Assert;

/**
 * @author Costin Leau
 * 
 */
public class MandatoryDependencyBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

	private static final Log log = LogFactory.getLog(MandatoryDependencyBeanPostProcessor.class);

	private MandatoryServiceDependencyManager manager;

	private ConfigurableListableBeanFactory beanFactory;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object,
	 * java.lang.String)
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ServiceExporter) {
			manager.addServiceExporter(beanName);
		}
		return bean;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		DefaultMandatoryDependencyManager manager = new DefaultMandatoryDependencyManager();
		manager.setBeanFactory(beanFactory);
		this.manager = manager;
	}

}
