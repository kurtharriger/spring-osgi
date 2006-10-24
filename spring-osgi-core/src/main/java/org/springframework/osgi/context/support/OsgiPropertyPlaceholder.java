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
package org.springframework.osgi.context.support;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.Assert;

/**
 * Osgi Property Placeholder. For the moment, this is just a dummy/no-op
 * implementation.
 * 
 * @author Costin Leau
 * 
 */
public class OsgiPropertyPlaceholder extends PropertyPlaceholderConfigurer implements BundleContextAware,
		InitializingBean {

	/**
	 * Map wrapper around a Dictionary.
	 * 
	 * TODO: is this class really needed? The dictionary is not updated so
	 * delegation doesn't add any value.
	 * 
	 * @author Costin Leau
	 * 
	 */
	private class DictionaryMapWrapper implements Map {
		private Dictionary dictionary;

		public DictionaryMapWrapper(Dictionary dict) {
			this.dictionary = dict;
		}

		public void clear() {
			throw new UnsupportedOperationException("not supported");
		}

		public boolean containsKey(Object key) {
			return (dictionary.get(key) == null);
		}

		public boolean containsValue(Object value) {
			throw new UnsupportedOperationException("not supported");
		}

		public Set entrySet() {
			final Enumeration keysEnum = dictionary.keys();
			Set keys = new HashSet();
			while (keysEnum.hasMoreElements()) {
				keys.add(new Map.Entry() {

					private Object key = keysEnum.nextElement();
					private Object value = dictionary.get(key);

					public Object getKey() {
						return key;
					}

					public Object getValue() {
						return value;
					}

					public Object setValue(Object value) {
						Object oldValue = this.value;
						this.value = value;
						return oldValue;
					}
				});
			}
			return keys;
		}

		public Object get(Object key) {
			return dictionary.get(key);
		}

		public boolean isEmpty() {
			return dictionary.isEmpty();
		}

		public Set keySet() {
			Enumeration keysEnum = dictionary.keys();
			Set keys = new HashSet();
			while (keysEnum.hasMoreElements())
				keys.add(keysEnum.nextElement());

			return keys;
		}

		public Object put(Object key, Object value) {
			return dictionary.put(key, value);
		}

		public void putAll(Map t) {
			for (Iterator iter = t.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				dictionary.put(entry.getKey(), entry.getValue());
			}
		}

		public Object remove(Object key) {
			return dictionary.remove(key);
		}

		public int size() {
			return dictionary.size();
		}

		public Collection values() {
			Enumeration valueEnum = dictionary.elements();
			List values = new ArrayList();
			while (valueEnum.hasMoreElements())
				values.add(valueEnum.nextElement());

			return values;
		}

	}

	private String persistentId;
	private BundleContext bundleContext;

	private Properties cmProperties;

	public String getPersistentId() {
		return persistentId;
	}

	public void setPersistentId(String persistentId) {
		this.persistentId = persistentId;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(persistentId, "peristentId property is required");
		Assert.notNull(bundleContext, "bundleContext property is required");

		ConfigurationAdmin admin = retrieveConfigurationAdmin(bundleContext);
		Configuration config = admin.getConfiguration(persistentId);

		// wrap configuration object as the backing properties of the
		// placeholder
		cmProperties = new Properties();
		Dictionary dict = config.getProperties();
		// copy dictionary into properties
		for (Enumeration keys = dict.keys(); keys.hasMoreElements();) {
			Object key = keys.nextElement();
			cmProperties.put(key, dict.get(key));
		}

		// add properties to the existing ones (to allow overriding rules to
		// apply)
	}

	protected Properties mergeProperties() throws IOException {
		Properties prop = super.mergeProperties();
		if (cmProperties != null)
			prop.putAll(cmProperties);
		return prop;
	}

	protected ConfigurationAdmin retrieveConfigurationAdmin(BundleContext bundleContext) {
		ServiceReference adminRef = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
		Assert.notNull(adminRef, "ConfigurationAdmin service reference could not be found");

		Object service = bundleContext.getService(adminRef);
		Assert.notNull(service, "ConfigurationAdmin Service could not be found");
		Assert.isInstanceOf(ConfigurationAdmin.class, service, "service " + service + " is not an instance of "
				+ ConfigurationAdmin.class.getName());

		return (ConfigurationAdmin) service;
	}

	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

}
