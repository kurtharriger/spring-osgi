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
import org.osgi.framework.*;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;

/**
 * An AbstractBundleXmlApplicationContext which delays initialization until all of its
 * osgi:reference targets are complete.
 * <p/> 
 *
 * @author Andy Piper
 * @author Hal Hildebrand
 */
public class ServiceDependentBundleXmlApplicationContext extends AbstractBundleXmlApplicationContext
		implements ServiceListener {
	private HashSet dependencies = new HashSet();
    private HashSet unsatisfiedDependencies = new HashSet();
	private Log log = LogFactory.getLog(ServiceDependentBundleXmlApplicationContext.class);
	private ClassLoader savedCcl;

	public ServiceDependentBundleXmlApplicationContext(BundleContext context,
                                                       String[] configLocations, ClassLoader classLoader,
                                                       NamespacePlugins plugins) {
		super(context, configLocations, classLoader, plugins);

		refreshBeanFactory();

        // Listen for services so that we can determine when we are ready for activation.
		findServiceDependencies();

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
            registerListener();
        }
    }

    public void close() {
        super.close();
        try {
			getBundleContext().removeServiceListener(this);
        } catch (IllegalStateException e) {
			logger.warn("exception thrown while removing service listener " + e);
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
            Dependency dependency = (Dependency) i.next();
            if (!dependency.isSatisfied()) {
                unsatisfiedDependencies.add(dependency);
                unsatisfiedServices = true;
            }
        }
        return unsatisfiedServices;
	}

	/**
	 * Look for osgi:reference's in this config.
	 * Deals with top level references as well as embedded references.
	 *
	 */
	protected void findServiceDependencies() {
		// Get bean types, but do not instantiate lazy singletons or templates
		String[] beans = getBeanFactory().getBeanDefinitionNames();
		HashSet d = new HashSet();

		for (int i = 0; i < beans.length; i++) {
			BeanDefinition bean = getBeanFactory().getBeanDefinition(beans[i]);
			if (!addBeanDependency(bean, d)) {
				// Check values for embedded references.
				PropertyValue[] values = bean.getPropertyValues().getPropertyValues();
				for (int p = 0; p < values.length; p++) {
					if (values[p].getValue() instanceof BeanDefinition) {
						addBeanDependency((BeanDefinition) values[p].getValue(), d);
					}
				}
			}
		}
		dependencies = d;
	}

	private boolean addBeanDependency(BeanDefinition bean, HashSet dependencies) {
		if (bean.getBeanClassName().equals(OsgiServiceProxyFactoryBean.class.getName())) {
			PropertyValue service =
                    bean.getPropertyValues().getPropertyValue(OsgiServiceProxyFactoryBean.INTERFACE_ATTRIBUTE);
			if (service == null) {
				throw new IllegalStateException("No interface specified for bean [" + bean + "]");
			}
			PropertyValue serviceFilter =
                    bean.getPropertyValues().getPropertyValue(OsgiServiceProxyFactoryBean.FILTER_ATTRIBUTE);
            String clazz = (String) service.getValue();
            String query =  serviceFilter != null ? (String) serviceFilter.getValue() : null;
            Filter filter = getFilter(clazz, query);
            dependencies.add(new Dependency(filter, clazz));
            return true;
		}
		return false;
	}

    private Filter getFilter (String serviceInterface, String serviceFilter) {
		StringBuffer sb = new StringBuffer();
		boolean andFilterWithInterfaceName = serviceFilter != null;
		if (andFilterWithInterfaceName) {
            sb.append("(&");
		}
		if (serviceFilter != null) {
			sb.append(serviceFilter);
		}
		sb.append("(");
		sb.append(Constants.OBJECTCLASS);
		sb.append("=");
		sb.append(serviceInterface);
		sb.append(")");

        if (andFilterWithInterfaceName) {
			sb.append(")");
		}
        try {
            return FrameworkUtil.createFilter(sb.toString());
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Filter string '"
                                               + serviceFilter
                                               + "' set on OsgiServiceProxyFactoryBean has invalid syntax: "
                                               + e.getMessage(), e);
        }
    }

    private void registerListener() {
        StringBuffer sb = new StringBuffer(100 * unsatisfiedDependencies.size());
        sb.append("(|");
        for (Iterator i = unsatisfiedDependencies.iterator(); i.hasNext();) {
            ((Dependency) i.next()).appendTo(sb);
        }
        sb.append(')');
        String filter = sb.toString();
        if (log.isInfoEnabled()) {
            log.info(getDisplayName() + " registering filter: " + filter);
        }
        try {
            getBundleContext().addServiceListener(this, filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Filter string '"
                                            + filter
                                            + "' has invalid syntax: "
                                            + e.getMessage(), e);
        }
    }

    /**
	 * Process serviceChanged events, completing context initialization if all
	 * the required dependencies are satisfied.
	 *
	 * @param serviceEvent
	 */
	public synchronized void serviceChanged(ServiceEvent serviceEvent) {
        if (unsatisfiedDependencies.isEmpty()) { // already completed.
			return;
		}
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.matches(serviceEvent)) {
                switch (serviceEvent.getType()) {
                    case ServiceEvent.MODIFIED:
                    case ServiceEvent.REGISTERED:
                        unsatisfiedDependencies.remove(dependency);
                        break;
                    case ServiceEvent.UNREGISTERING:
                        unsatisfiedDependencies.add(dependency);
                        break;
                    default: // do nothing
                        break;
                }
            }
        }
		// Good to go!
		if (unsatisfiedDependencies.isEmpty()) {
			if (log.isInfoEnabled()) {
				log.info("Services satisfied, completing initialization for " + getDisplayName());
			}
			completeContextInitialization();
			notifyAll();
		}else {
            registerListener();  // re-register with the new filter
        }
	}

	public synchronized boolean isAvailable() {
		return unsatisfiedDependencies.isEmpty();
	}

    private class Dependency {
        private final Filter filter;
        private final String clazz;

        private Dependency(Filter filter, String clazz) {
            this.filter = filter;
            this.clazz = clazz;
        }

        public boolean matches(ServiceEvent event) {
            return filter.match(event.getServiceReference());
        }

        public boolean isSatisfied() {
            ServiceReference[] refs;
            try {
                refs = getBundleContext().getServiceReferences(clazz, filter.toString());
            } catch (InvalidSyntaxException e) {
                throw new IllegalStateException("Filter string '"
                                                + filter.toString()
                                                + "' has invalid syntax: "
                                                + e.getMessage(), e);
            }
            return refs != null && refs.length != 0;
        }

        public String toString() {
            return "Dependency on [" + filter + "]";
        }

        public void appendTo(StringBuffer sb) {
            sb.append(filter);
        }
    }
}
