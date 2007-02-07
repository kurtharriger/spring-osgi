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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Costin Leau
 * 
 */
public class MockServiceReference implements ServiceReference {

	private Bundle bundle;

	// private ServiceRegistration registration;
	private Dictionary properties;

	private String[] objectClass = new String[] { Object.class.getName() };

	public MockServiceReference() {
		this(null, null, null);
	}

	public MockServiceReference(Bundle bundle) {
		this(bundle, null, null);
	}

	public MockServiceReference(String[] classes) {
		this(null, null, null, classes);

	}

	public MockServiceReference(Bundle bundle, String[] classes) {
		this(bundle, null, null, classes);
	}

	public MockServiceReference(ServiceRegistration registration) {
		this(null, null, registration);
	}

	public MockServiceReference(Bundle bundle, Dictionary properties, ServiceRegistration registration) {
		this(bundle, properties, registration, null);
	}

	public MockServiceReference(Bundle bundle, Dictionary properties, ServiceRegistration registration, String[] classes) {
		this.bundle = (bundle == null ? new MockBundle() : bundle);
		// this.registration = (registration == null ? new
		// MockServiceRegistration() :
		// registration);
		this.properties = (properties == null ? new Hashtable() : properties);
		if (classes != null && classes.length > 0)
			this.objectClass = classes;
		addMandatoryProperties(this.properties);
	}

	private void addMandatoryProperties(Dictionary dict) {
		// add mandatory properties
		if (dict.get(Constants.SERVICE_ID) == null)
			dict.put(Constants.SERVICE_ID, new Long(System.currentTimeMillis()));

		if (dict.get(Constants.OBJECTCLASS) == null)
			dict.put(Constants.OBJECTCLASS, objectClass);

		if (dict.get(Constants.SERVICE_RANKING) == null)
			dict.put(Constants.SERVICE_RANKING, new Integer(0));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.ServiceReference#getBundle()
	 */
	public Bundle getBundle() {
		return bundle;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.ServiceReference#getProperty(java.lang.String)
	 */
	public Object getProperty(String key) {
		return properties.get(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.ServiceReference#getPropertyKeys()
	 */
	public String[] getPropertyKeys() {
		String[] keys = new String[this.properties.size()];
		Enumeration ks = this.properties.keys();

		for (int i = 0; i < keys.length && ks.hasMoreElements(); i++) {
			keys[i] = (String) ks.nextElement();
		}

		return keys;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.ServiceReference#getUsingBundles()
	 */
	public Bundle[] getUsingBundles() {
		return new Bundle[] {};
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.ServiceReference#isAssignableTo(org.osgi.framework.Bundle,
	 * java.lang.String)
	 */
	public boolean isAssignableTo(Bundle bundle, String className) {
		return false;
	}

	public void setProperties(Dictionary properties) {
		/*
		 * Enumeration keys = props.keys(); while (keys.hasMoreElements())
		 * this.properties.remove(keys.nextElement());
		 * 
		 * Enumeration enm = props.keys(); while (enm.hasMoreElements()) {
		 * Object key = enm.nextElement(); this.properties.put(key,
		 * props.get(key)); }
		 */

		if (properties != null) {
			// copy mandatory properties
			properties.put(Constants.SERVICE_ID, this.properties.get(Constants.SERVICE_ID));
			properties.put(Constants.OBJECTCLASS, this.properties.get(Constants.OBJECTCLASS));
			properties.put(Constants.SERVICE_RANKING, this.properties.get(Constants.SERVICE_RANKING));
			
			this.properties = properties;
		}
	}
}
