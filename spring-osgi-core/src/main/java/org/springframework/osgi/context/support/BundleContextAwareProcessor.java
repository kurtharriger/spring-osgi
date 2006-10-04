/*
 * Copyright 2002-2006 the original author or authors.
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
 *
 * Created on 23-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.osgi.context.BundleContextAware;

/**
 * For internal use only. Used by OsgiBundleXmlApplicationContext to 
 * inject beans implementing BundleContextAware with a reference to the
 * current BundleContext.
 * 
 * @author Adrian Colyer
 * @since 2.0
 */
public class BundleContextAwareProcessor implements BeanPostProcessor {

	private BundleContext bundleContext;
	
	protected final Log logger = LogFactory.getLog(getClass());

	public BundleContextAwareProcessor(BundleContext aContext) {
		this.bundleContext = aContext;
	}
	
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessBeforeInitialization(java.lang.Object, java.lang.String)
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof BundleContextAware) {
			if (this.bundleContext == null) {
				throw new IllegalStateException("Cannot satisfy BundleContextAware for bean '" +
						beanName + "' without BundleContext");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Invoking setBundleContext on BundleContextAware bean '" + beanName + "'");
			}
			((BundleContextAware) bean).setBundleContext(this.bundleContext);
		}
		return bean;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.config.BeanPostProcessor#postProcessAfterInitialization(java.lang.Object, java.lang.String)
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

}
