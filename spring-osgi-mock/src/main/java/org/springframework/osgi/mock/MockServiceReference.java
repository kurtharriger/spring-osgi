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
import org.osgi.framework.ServiceReference;

/**
 * @author Costin Leau
 * 
 */
public class MockServiceReference implements ServiceReference {

	private Hashtable properties;
	private Bundle bundle;

	public MockServiceReference() {
		this(new MockBundle(), new Hashtable());
	}

	public MockServiceReference(Bundle bundle) {
		this(bundle, new Hashtable());
	}

	public MockServiceReference(Bundle bundle, Hashtable props) {
		this.bundle = bundle;
		this.properties = props;
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
	 *      java.lang.String)
	 */
	public boolean isAssignableTo(Bundle bundle, String className) {
		return false;
	}

	public void setProperties(Dictionary properties) {
		this.properties.clear();

		Enumeration enm = properties.keys();
		while (enm.hasMoreElements()) {
			Object key = enm.nextElement();
			this.properties.put(key, properties.get(key));
		}
	}

}
