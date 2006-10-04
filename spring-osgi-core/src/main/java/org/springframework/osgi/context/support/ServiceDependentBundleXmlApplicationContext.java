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
 */
package org.springframework.osgi.context.support;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;

/**
 * An AbstractBundleXmlApplicationContext which delays initialization until all of its
 * osgi:reference targets are complete.
 * <p/>
 * TODO deal with service filtering.
 *
 * @author Andy Piper
 */
public class ServiceDependentBundleXmlApplicationContext extends AbstractBundleXmlApplicationContext
		implements ServiceListener
{
	private HashSet dependencies = new HashSet();
	private HashSet unsatisfiedDependencies = new HashSet();
	private Log log = LogFactory.getLog(ServiceDependentBundleXmlApplicationContext.class);
	private ClassLoader savedCcl;

	public ServiceDependentBundleXmlApplicationContext(ApplicationContext parent, BundleContext context,
                                                       String[] configLocations, ClassLoader classLoader,
                                                       NamespacePlugins plugins) {
		super(parent, context, configLocations, classLoader, plugins);

		refreshBeanFactory();
		// Listen for services so that we can determine when we are ready for activation.
		dependencies = findServiceDependencies();
		// Check to see if we have a dependent parent context.
		if (parent == null) {
			String parentContextServiceName = (String)context.getBundle().getHeaders().get(PARENT_CONTEXT_SERVICE_NAME_HEADER);
			if (parentContextServiceName != null) {
				dependencies.add(parentContextServiceName);
			}
		}
	   // If we have dependencies then set up an appropriate filter.
		if (!dependencies.isEmpty()) {
			// Create a super-filter matching all our required classes.
			StringBuffer filterString = new StringBuffer();
			if (dependencies.size() > 1) {
				filterString.append("(&");
			}
			for (Iterator i = dependencies.iterator(); i.hasNext();) {
				String clazz = (String) i.next();
				filterString.append("(").append(Constants.OBJECTCLASS).append("=").append(clazz).append(")");
			}
			if (dependencies.size() > 1) {
				filterString.append(")");
			}
			if (log.isInfoEnabled()) {
				log.info("Service dependencies exist, registering listener with filter ["
						+ filterString.toString() + "]");
			}
			// Add a service listener before checking for satisfied dependencies, to avoid missing
			// any important events.
			try {
				getBundleContext().addServiceListener(this, filterString.toString());
			} catch (InvalidSyntaxException e) {
				throw new IllegalArgumentException("Bad filter: [" + filterString.toString() + "]", e);
			}
		}

		if (!hasUnsatisifiedServiceDependencies()) {
			if (log.isInfoEnabled()) {
				log.info("Services already satisfied, completing initialization for " + getDisplayName());
			}
			dependencies.clear();
			getBundleContext().removeServiceListener(this);
			refresh();
			publishContextAsOsgiService();
		} else {
			savedCcl = Thread.currentThread().getContextClassLoader();
		}
	}

	/**
	 * Complete the initialization of this context now that we know all services
	 * are available.
	 */
	protected void completeContextInitialization() {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		BundleContext bc = LocalBundleContext.getContext();
		try {
			Thread.currentThread().setContextClassLoader(savedCcl);
			LocalBundleContext.setContext(getBundleContext());
			dependencies.clear();
			getBundleContext().removeServiceListener(this);
			// Parent will be in our dependencies if it exist.
			if (getParent() == null) {
				setParent(getParentApplicationContext(getBundleContext()));
			}
			// Build the bean definitions.
			refresh();
			publishContextAsOsgiService();

		} finally {
			LocalBundleContext.setContext(bc);
			Thread.currentThread().setContextClassLoader(ccl);
		}
	}

	protected synchronized boolean hasUnsatisifiedServiceDependencies() {
		boolean unsatisfiedServices = false;
		for (Iterator i = dependencies.iterator(); i.hasNext();) {
			String filter = null;
			String clazz = (String) i.next();
			try {
				ServiceReference[] serviceReferences =
						getBundleContext().getServiceReferences(clazz, filter);
				if (serviceReferences == null || serviceReferences.length == 0) {
					unsatisfiedDependencies.add(clazz);
					unsatisfiedServices = true;
				}
			} catch (InvalidSyntaxException e) {
				throw new IllegalArgumentException("Bad filter definition [" + filter +
						"] for bean [" + clazz + "]");
			}
		}
		return unsatisfiedServices;
	}

	/**
	 * Look for osgi:reference's in this config.
	 * Deals with top level references as well as embedded references.
	 *
	 * @return the list of dependant references.
	 */
	protected HashSet findServiceDependencies() {
		// Get bean types, but do not instantiate lazy singletons or templates
		String[] beans = getBeanFactory().getBeanDefinitionNames();
		HashSet dependencies = new HashSet();

		for (int i = 0; i < beans.length; i++) {
			BeanDefinition bean = getBeanFactory().getBeanDefinition(beans[i]);
			if (!addBeanDependency(bean, dependencies)) {
				// Check values for embedded references.
				PropertyValue[] values = bean.getPropertyValues().getPropertyValues();
				for (int p = 0; p < values.length; p++) {
					if (values[p].getValue() instanceof BeanDefinition) {
						addBeanDependency((BeanDefinition) values[p].getValue(), dependencies);
					}
				}
			}
		}
		return dependencies;
	}

	private boolean addBeanDependency(BeanDefinition bean, HashSet dependencies) {
		if (bean.getBeanClassName().equals(OsgiServiceProxyFactoryBean.class.getName())) {
			PropertyValue service = bean.getPropertyValues().getPropertyValue
					(OsgiServiceProxyFactoryBean.INTERFACE_ATTRIBUTE);
			if (service == null) {
				throw new IllegalStateException("No interface specified for bean [" + bean + "]");
			}
			dependencies.add(service.getValue());
			PropertyValue filter = bean.getPropertyValues().getPropertyValue
					(OsgiServiceProxyFactoryBean.FILTER_ATTRIBUTE);
			return true;
		}
		return false;
	}

	/**
	 * Process serviceChanged events, completing context initialization if all
	 * the required dependencies are satisfied.
	 *
	 * @param serviceEvent
	 */
	public synchronized void serviceChanged(ServiceEvent serviceEvent) {
		if (dependencies.isEmpty()) { // already completed.
			return;
		}
		String[] classes = (String[]) serviceEvent.getServiceReference()
				.getProperty(Constants.OBJECTCLASS);
		switch (serviceEvent.getType()) {
			case ServiceEvent.REGISTERED:
				for (int i = 0; i < classes.length; i++) {
					// FIXME andyp -- deal with service filtering here
					unsatisfiedDependencies.remove(classes[i]);
				}
				break;
			case ServiceEvent.UNREGISTERING:
				for (int i = 0; i < classes.length; i++) {
					if (dependencies.contains(classes[i])) {
						unsatisfiedDependencies.add(classes[i]);
					}
				}
				break;
			default: // do nothing
				break;
		}
		// Good to go!
		if (unsatisfiedDependencies.isEmpty()) {
			if (log.isInfoEnabled()) {
				log.info("Services satisfied, completing initialization for " + getDisplayName());
			}
			completeContextInitialization();
			notifyAll();
		}
		// Don't check for the inverse case - we assume that the OsgiProxyFactoryBean will cope with
		// missing services.
	}

	public synchronized boolean isAvailable() {
		return unsatisfiedDependencies.isEmpty();
	}
}
