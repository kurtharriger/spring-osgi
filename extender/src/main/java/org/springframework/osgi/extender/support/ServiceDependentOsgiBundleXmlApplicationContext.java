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
package org.springframework.osgi.extender.support;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.service.CardinalityOptions;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;

import java.util.*;

/**
 * An OsgiBundleXmlApplicationContext which delays initialization until all
 * of its osgi:reference targets are complete. <p/>
 *
 * @author Andy Piper
 * @author Hal Hildebrand
 * @author Costin Leau
 */
public class ServiceDependentOsgiBundleXmlApplicationContext extends OsgiBundleXmlApplicationContext {
    private volatile ContextState state = ContextState.INITIALIZED;
    private DependencyListener listener;

    private static final Log log = LogFactory.getLog(ServiceDependentOsgiBundleXmlApplicationContext.class);


    public ServiceDependentOsgiBundleXmlApplicationContext(BundleContext context, String[] configLocations) {
        super(context, configLocations);
    }

    protected synchronized void create(final Runnable postAction) throws BeansException {
        preRefresh();
        DependencyListener dl = new DependencyListener(postAction);
        dl.findServiceDependencies();

        state = ContextState.RESOLVING_DEPENDENCIES;
        if (!dl.isSatisfied()) {
            listener = dl;
            listener.register();
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No outstanding dependencies, completing initialization for "
                          + getDisplayName());
            } 
            postAction.run();
        }
    }

    public synchronized void close() {
        state = ContextState.CLOSED;
        if (listener != null) {
            listener.deregister();
        }
        super.close();
    }


    protected void interrupt() {
        state = ContextState.INTERRUPTED;
        if (listener != null) {
            listener.deregister();
        }
    }

    protected void complete() {
        postRefresh();
    }


    public boolean isAvailable() {
        return isActive() && (state == ContextState.DEPENDENCIES_RESOLVED);
    }


    /**
     * ServiceListener used for tracking dependent services. As the
     * ServiceListener receives event synchronously there is no need for
     * synchronization.
     *
     * @author Costin Leau
     */
    protected class DependencyListener implements ServiceListener {
        private final Set dependencies = new LinkedHashSet();
        private final Set unsatisfiedDependencies = new LinkedHashSet();
        private final Runnable postAction;


        protected DependencyListener(Runnable postAction) {
            this.postAction = postAction;
        }

        protected void findServiceDependencies() {
            ConfigurableListableBeanFactory beanFactory = getBeanFactory();
            String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
                                                                                 OsgiServiceProxyFactoryBean.class,
                                                                                 true,
                                                                                 true);
            for (int i = 0; i < beans.length; i++) {
                String beanName = BeanFactory.FACTORY_BEAN_PREFIX + beans[i];
                OsgiServiceProxyFactoryBean reference = (OsgiServiceProxyFactoryBean) beanFactory.getBean(beanName);
                Dependency dependency = new Dependency(reference);
                dependencies.add(dependency);
                if (!dependency.isSatisfied()) {
                    unsatisfiedDependencies.add(dependency);
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug(dependencies.size() +
                          " dependencies, " +
                          unsatisfiedDependencies.size() +
                          " unsatisfied for " +
                          getDisplayName());
            }

        }


        protected boolean isSatisfied() {
            return unsatisfiedDependencies.isEmpty();
        }


        protected void register() {
            boolean multiple = unsatisfiedDependencies.size() > 1;
            StringBuffer sb = new StringBuffer(100 * unsatisfiedDependencies.size());
            if (multiple) {
                sb.append("(|");
            }
            for (Iterator i = unsatisfiedDependencies.iterator(); i.hasNext();) {
                ((Dependency) i.next()).appendTo(sb);
            }
            if (multiple) {
                sb.append(')');
            }
            String filter = sb.toString();
            if (log.isInfoEnabled()) {
                log.info(getDisplayName() + " registering listener with filter: " + filter);
            }
            try {
                getBundleContext().addServiceListener(this, filter);
            }
            catch (InvalidSyntaxException e) {
                throw (IllegalStateException) new IllegalStateException("Filter string '" + filter
                                                                        + "' has invalid syntax: "
                                                                        + e.getMessage()).initCause(e);
            }
        }

        protected void deregister() {
            try {
                getBundleContext().removeServiceListener(this);
            } catch (IllegalStateException e) {
                // Bundle context is no longer valid
            }
        }


        /**
         * Process serviceChanged events, completing context initialization if
         * all the required dependencies are satisfied.
         *
         * @param serviceEvent
         */
        public void serviceChanged(ServiceEvent serviceEvent) {
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
                            if (log.isDebugEnabled()) {
                                log.debug("found service; eliminating " + dependency);
                            }
                            break;
                        case ServiceEvent.UNREGISTERING:
                            unsatisfiedDependencies.add(dependency);
                            if (log.isDebugEnabled()) {
                                log.debug("service unregistered; adding " + dependency);
                            }
                            break;
                        default: // do nothing
                            break;
                    }
                }
            }

            if (state == ContextState.INTERRUPTED || state == ContextState.CLOSED) {
                deregister();
                return;
            }

            // Good to go!
            if (unsatisfiedDependencies.isEmpty()) {
                deregister();
                listener = null;
                state = ContextState.DEPENDENCIES_RESOLVED;
                if (log.isDebugEnabled()) {
                    log.debug("No outstanding dependencies, completing initialization for "
                              + getDisplayName());
                }
                postAction.run();
            } else {
                register(); // re-register with the new filter
            }
        }
    }

    protected class Dependency {
        private final String filterString;
        private final Filter filter;
        private final int cardinality;


        private Dependency(OsgiServiceProxyFactoryBean reference) {
            filter = reference.getUnifiedFilter();
            filterString = filter.toString();
            cardinality = reference.getCard();
        }


        public boolean matches(ServiceEvent event) {
            return filter.match(event.getServiceReference());
        }


        public void appendTo(StringBuffer sb) {
            sb.append(filterString);
        }


        public boolean isSatisfied() {
            ServiceReference[] refs;
            try {
                refs = getBundleContext().getServiceReferences(null, filterString);
            }
            catch (InvalidSyntaxException e) {
                throw (IllegalStateException) new IllegalStateException("Filter '" + filterString
                                                                        + "' has invalid syntax: "
                                                                        + e.getMessage()).initCause(e);
            }
            return !CardinalityOptions.atLeastOneRequired(cardinality) || (refs != null && refs.length != 0);
        }


        public String toString() {
            return "Dependency on [" + filter + "]";
        }


        /**
         * @return Returns the filter.
         */
        public Filter getFilter() {
            return filter;
        }

    }

}
