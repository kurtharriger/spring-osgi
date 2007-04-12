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


import edu.emory.mathcs.backport.java.util.concurrent.BrokenBarrierException;
import edu.emory.mathcs.backport.java.util.concurrent.CyclicBarrier;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.*;
import org.springframework.beans.BeansException; 
import org.springframework.context.ApplicationContextException;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.service.CardinalityOptions;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.util.ConfigUtils;

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
    private boolean dependenciesSatisfied = false;
    private long timeout = ConfigUtils.DIRECTIVE_TIMEOUT_DEFAULT;
    private Thread refreshWorker;

    private static final Log log = LogFactory.getLog(ServiceDependentOsgiBundleXmlApplicationContext.class);


    public ServiceDependentOsgiBundleXmlApplicationContext(BundleContext context, String[] configLocations) {
        super(context, configLocations);
    } 


    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void close() {
        if (refreshWorker != null) {
            refreshWorker.interrupt();
            // refreshWorker.stop();
            refreshWorker = null;
        }
        super.close();
    }

    public void refresh() {
        refreshWorker = Thread.currentThread();
        try {
            super.refresh();
        } finally {
            refreshWorker = null;
        }
    }


    /*
    * (non-Javadoc)
    * @see org.springframework.context.support.AbstractApplicationContext#onRefresh()
    */
    protected void onRefresh() throws BeansException {
        CyclicBarrier barrier = new CyclicBarrier(2);
        DependencyListener listener = new DependencyListener(barrier);
        listener.findServiceDependencies();

        if (!listener.getUnsatisfiedDependencies().isEmpty()) {
            listener.register();
            try {
                if (timeout == ConfigUtils.DIRECTIVE_NO_TIMEOUT) {
                    barrier.await();
                } else {
                    barrier.await(timeout, TimeUnit.SECONDS);
                }
            } catch (BrokenBarrierException e) {
                throw new ApplicationContextException("Unable to complete application context initializition for '"
                                                      + getBundle().getSymbolicName()
                                                      + "'", e);
            } catch (InterruptedException e) {
                throw new ApplicationContextException("Thread interrupted", e);

            } catch (TimeoutException e) {
                throw new ApplicationContextException("Unable to complete application context initializition for '"
                                                      + getBundle().getSymbolicName()
                                                      + "'", e);
            } finally {
                getBundleContext().removeServiceListener(listener);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No outstanding dependencies, completing initialization for " + getDisplayName());
            }
        }
        dependenciesSatisfied = true;
        super.onRefresh();
    }


    public boolean isAvailable() {
        return isActive() && dependenciesSatisfied;
    }


    /**
     * ServiceListener used for tracking dependent services. As the
     * ServiceListener receives event synchronously there is no need for
     * synchronization.
     *
     * @author Costin Leau
     */
    private class DependencyListener implements ServiceListener {
        private CyclicBarrier barrier;
        private final Set dependencies = new LinkedHashSet();
        private final Set unsatisfiedDependencies = new LinkedHashSet();


        private DependencyListener(CyclicBarrier barrier) {
            this.barrier = barrier;
        }


        private void findServiceDependencies() {
            for (Iterator i = references.iterator(); i.hasNext(); ) { 
                OsgiServiceProxyFactoryBean reference = (OsgiServiceProxyFactoryBean) i.next();
                Dependency dependency = new Dependency(reference);
                dependencies.add(dependency);
                if (!dependency.isSatisfied()) {
                    unsatisfiedDependencies.add(dependency);
                }
            }

        }


        private Set getUnsatisfiedDependencies() {
            return unsatisfiedDependencies;
        }


        private void register() {
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
                                log.debug("found service; eliminating dependency =" + dependency);
                            }
                            break;
                        case ServiceEvent.UNREGISTERING:
                            unsatisfiedDependencies.add(dependency);
                            if (log.isDebugEnabled()) {
                                log.debug("service unregistered; adding dependency =" + dependency);
                            }
                            break;
                        default: // do nothing
                            break;
                    }
                }
            }
            // Good to go!
            if (unsatisfiedDependencies.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Dependent services now satisfied, completing initialization for " + getDisplayName());
                }
                try {
                    barrier.await();
                } catch (BrokenBarrierException e) {
                    log.error("Unable to complete application context initializition for '"
                              + getBundle().getSymbolicName()
                              + "'", e);
                } catch (InterruptedException e) {
                    // Outta here!  No logging! 
                }

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
