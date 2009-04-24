/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.compendium.internal.cm;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.util.internal.MapBasedDictionary;
import org.springframework.util.Assert;

/**
 * Utility class for the Configuration Admin package.
 * 
 * @author Costin Leau
 */
public abstract class CMUtils {

	/**
	 * Injects the properties from the given Map to the given object.
	 * Additionally, a bean factory can be passed in for copying property
	 * editors inside the injector.
	 * 
	 * @param instance bean instance to configure
	 * @param properties
	 * @param beanFactory
	 */
	public static void applyMapOntoInstance(Object instance, Map<String, ?> properties, AbstractBeanFactory beanFactory) {
		if (properties != null && !properties.isEmpty()) {
			BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(instance);
			// configure bean wrapper (using method from Spring 2.5.6)
			if (beanFactory != null) {
				beanFactory.copyRegisteredEditorsTo(beanWrapper);
			}
			for (Iterator<?> iterator = properties.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry<String, ?> entry = (Map.Entry<String, ?>) iterator.next();
				String propertyName = entry.getKey();
				if (beanWrapper.isWritableProperty(propertyName)) {
					beanWrapper.setPropertyValue(propertyName, entry.getValue());
				}
			}
		}
	}

	public static void bulkUpdate(UpdateCallback callback, Collection<?> instances, Map<?, ?> properties) {
		for (Iterator<?> iterator = instances.iterator(); iterator.hasNext();) {
			Object instance = iterator.next();
			callback.update(instance, properties);
		}
	}

	public static UpdateCallback createCallback(UpdateStrategy strategy, String methodName, BeanFactory beanFactory) {
		if (UpdateStrategy.BEAN_MANAGED.equals(strategy)) {
			Assert.hasText(methodName, "method name required when using 'bean-managed' strategy");
			return new BeanManagedUpdate(methodName);
		}
		else if (UpdateStrategy.CONTAINER_MANAGED.equals(strategy)) {
			return new ContainerManagedUpdate(beanFactory);
		}

		return null;
	}

	public static Map getConfiguration(BundleContext bundleContext, String pid) throws IOException {
		ServiceReference ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
		if (ref != null) {
			ConfigurationAdmin cm = (ConfigurationAdmin) bundleContext.getService(ref);
			if (cm != null) {
				return new MapBasedDictionary(cm.getConfiguration(pid).getProperties());
			}
		}
		return Collections.EMPTY_MAP;
	}

	public static ServiceRegistration registerManagedService(BundleContext bundleContext, ManagedService listener,
			String pid) {

		Properties props = new Properties();
		props.put(Constants.SERVICE_PID, pid);
		Bundle bundle = bundleContext.getBundle();
		props.put(Constants.BUNDLE_SYMBOLICNAME, OsgiStringUtils.nullSafeSymbolicName(bundle));
		props.put(Constants.BUNDLE_VERSION, OsgiBundleUtils.getBundleVersion(bundle));

		return bundleContext.registerService(ManagedService.class.getName(), listener, props);
	}
}