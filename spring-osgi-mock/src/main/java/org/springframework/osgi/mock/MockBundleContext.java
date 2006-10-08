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
 */
package org.springframework.osgi.mock;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Costin Leau
 * 
 */
public class MockBundleContext implements BundleContext {

	public static final Properties DEFAULT_PROPERTIES = new Properties();

	static {
		DEFAULT_PROPERTIES.put(Constants.FRAMEWORK_VERSION, "1.0-SNAPSHOT");
		DEFAULT_PROPERTIES.put(Constants.FRAMEWORK_VENDOR, "Interface21");
		DEFAULT_PROPERTIES.put(Constants.FRAMEWORK_LANGUAGE, System.getProperty("file.encoding"));
		DEFAULT_PROPERTIES.put(Constants.FRAMEWORK_OS_NAME, System.getProperty("os.name"));
		DEFAULT_PROPERTIES.put(Constants.FRAMEWORK_OS_VERSION, System.getProperty("os.version"));
		DEFAULT_PROPERTIES.put(Constants.FRAMEWORK_PROCESSOR, System.getProperty("os.arch"));
	}

	private Bundle bundle;
	private Properties properties;

	public MockBundleContext() {
		this(null, null);
	}

	public MockBundleContext(Bundle bundle) {
		this(bundle, null);
	}

	public MockBundleContext(Bundle bundle, Properties props) {
		this.bundle = (bundle == null ? new MockBundle(this) : bundle);
		properties = new Properties(DEFAULT_PROPERTIES);
		if (props != null)
			properties.putAll(props);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
	 */
	public void addBundleListener(BundleListener listener) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
	 */
	public void addFrameworkListener(FrameworkListener listener) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener)
	 */
	public void addServiceListener(ServiceListener listener) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener,
	 *      java.lang.String)
	 */
	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
	 */
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		return new MockFilter(filter);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String,
	 *      java.lang.String)
	 */
	public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return new ServiceReference[] {};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getBundle()
	 */
	public Bundle getBundle() {
		return bundle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getBundle(long)
	 */
	public Bundle getBundle(long id) {
		return bundle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getBundles()
	 */
	public Bundle[] getBundles() {
		return new Bundle[] { bundle };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
	 */
	public File getDataFile(String filename) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
	 */
	public Object getService(ServiceReference reference) {
		return new Object();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
	 */
	public ServiceReference getServiceReference(String clazz) {
		return new MockServiceReference(getBundle());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String,
	 *      java.lang.String)
	 */
	public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return new ServiceReference[] { new MockServiceReference(getBundle()) };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
	 */
	public Bundle installBundle(String location) throws BundleException {
		return new MockBundle(location);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#installBundle(java.lang.String,
	 *      java.io.InputStream)
	 */
	public Bundle installBundle(String location, InputStream input) throws BundleException {
		return new MockBundle(location);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#registerService(java.lang.String[],
	 *      java.lang.Object, java.util.Dictionary)
	 */
	public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
		return new MockServiceRegistration();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#registerService(java.lang.String,
	 *      java.lang.Object, java.util.Dictionary)
	 */
	public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
		return new MockServiceRegistration();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
	 */
	public void removeBundleListener(BundleListener listener) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
	 */
	public void removeFrameworkListener(FrameworkListener listener) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
	 */
	public void removeServiceListener(ServiceListener listener) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
	 */
	public boolean ungetService(ServiceReference reference) {
		return false;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

}
