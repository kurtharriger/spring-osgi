/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.springframework.osgi.context;

import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.osgi.context.support.AbstractBundleXmlApplicationContext;
import org.springframework.osgi.context.support.DefaultOsgiBundleXmlApplicationContextFactory;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContextFactory;

/**
 * @author Andy Piper
 * @since 2.1
 */
public class OsgiBundleXmlApplicationContextFactoryBean implements FactoryBean, BundleContextAware,
	BeanNameAware, ApplicationContextAware, InitializingBean {
	private BundleContext context;
	private ApplicationContext parent;
	private OsgiBundleXmlApplicationContextFactory contextFactory = new DefaultOsgiBundleXmlApplicationContextFactory();
	private String name;
	private Resource configLocation;

	public Object getObject() throws Exception {
		return contextFactory.createApplicationContextWithBundleContext(context,
			new String[]{configLocation.getURL().toString()}, ContextLoaderListener.plugins(), false);
	}

	public Class getObjectType() {
		return AbstractBundleXmlApplicationContext.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void setBundleContext(BundleContext context) {
		this.context = context;
	}

	public void setBeanName(String name) {
		this.name = name;
	}

	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		parent = applicationContext;
	}

	public void afterPropertiesSet() throws Exception {
		getObject();
	}
}
