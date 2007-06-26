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
 * Created on 25-Jan-2006 by Adrian Colyer
 */
package org.springframework.osgi.service;

import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.service.exporter.OsgiServiceFactoryBean;
import org.springframework.osgi.util.MapBasedDictionary;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * OsgiServicePropertiesResolver that creats a service property set with the
 * following properties:
 * <ul>
 * <li>Bundle-SymbolicName=&lt;bundle symbolic name&gt;</li>
 * <li>Bundle-Version=&lt;bundle version&gt;</li>
 * <li>org.springframework.osgi.beanname="&lt;bean name&gt;</li>
 * </ul>
 * 
 * @see OsgiServicePropertiesResolver
 * @see OsgiServiceFactoryBean
 * 
 * @author Adrian Colyer
 * @author Hal Hildebrand
 */
public class BeanNameServicePropertiesResolver implements OsgiServicePropertiesResolver, BundleContextAware,
		InitializingBean {

	public static final Object BEAN_NAME_PROPERTY_KEY = "org.springframework.osgi.beanname";
	private BundleContext bundleContext;

	public BeanNameServicePropertiesResolver() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.service.OsgiServicePropertiesResolver#getServiceProperties(java.lang.Object,
	 *      java.lang.String)
	 */
	public Map getServiceProperties(String beanName) {
		Map p = new MapBasedDictionary();
		p.put(BEAN_NAME_PROPERTY_KEY, beanName);		
		String name = getSymbolicName();
		if (StringUtils.hasLength(name)) {
			p.put(Constants.BUNDLE_SYMBOLICNAME, name);
		}
		String version = getBundleVersion();
		if (StringUtils.hasLength(version)) {
			p.put(Constants.BUNDLE_VERSION, version);
		}
		return p;
	}

	private String getBundleVersion() {
		return (String) this.bundleContext.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
	}

	private String getSymbolicName() {
		return this.bundleContext.getBundle().getSymbolicName();
	}

	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(bundleContext, "required property bundleContext has not been set");
	}

}
