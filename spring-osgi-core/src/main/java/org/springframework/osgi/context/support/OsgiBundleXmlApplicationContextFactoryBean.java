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
package org.springframework.osgi.context.support;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.osgi.context.BundleContextAware;

/**
 * @author Andy Piper
 * @since 2.1
 */
public class OsgiBundleXmlApplicationContextFactoryBean implements FactoryBean, BundleContextAware,
	InitializingBean, DisposableBean {
	private BundleContext context;
	private OsgiBundleXmlApplicationContextFactory contextFactory = new DefaultOsgiBundleXmlApplicationContextFactory();
	private Resource configLocation;
	private ServiceReference resolverServiceReference;
	private OsgiBundleNamespaceHandlerAndEntityResolver resolver;

	public Object getObject() throws Exception {
		ConfigurableApplicationContext appContext = contextFactory.createApplicationContextWithBundleContext(context,
			new String[]{configLocation.getURL().toString()}, resolver, new SyncTaskExecutor(), false);
		appContext.refresh();
		return appContext;
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

	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	public void afterPropertiesSet() throws Exception {
		if (this.context == null) {
			throw new IllegalStateException("Required property bundle context has not been set");
		}
		this.resolverServiceReference = this.context.getServiceReference(OsgiBundleNamespaceHandlerAndEntityResolver.class.getName());
		if (this.resolverServiceReference == null) {
			throw new BeanCreationException("Required Namespace Handler and Entity Resolver OSGi service could not be found," + 
					 " perhaps the org.springframework.osgi.extender bundle has not been installed and started?");
		}
		this.resolver = (OsgiBundleNamespaceHandlerAndEntityResolver) this.context.getService(this.resolverServiceReference);
		getObject();
	}

	public void destroy() throws Exception {
		if (this.resolverServiceReference != null) {
			this.context.ungetService(this.resolverServiceReference);
		}		
	}
}
