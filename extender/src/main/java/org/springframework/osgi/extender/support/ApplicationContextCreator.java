/*
 * Copyright 2006 the original author or authors.
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
package org.springframework.osgi.extender.support;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.ApplicationContextException;
import org.springframework.osgi.context.support.ApplicationContextConfiguration;
import org.springframework.osgi.context.support.BundleDelegatingClassLoader;
import org.springframework.osgi.context.support.LocalBundleContext;
import org.springframework.osgi.context.support.NamespacePlugins; 
import org.springframework.osgi.context.support.SpringBundleEvent;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.core.task.TaskExecutor;

/**
 * Search a bundle for Spring resources, and if found create an application
 * context for it.
 * 
 * @author Adrian Colyer
 */
public class ApplicationContextCreator {
    
    protected static final Log log = LogFactory.getLog(ApplicationContextCreator.class);

	protected final Bundle bundle;

	protected final Map applicationContextMap;

	protected final Map contextsPendingInitializationMap;

	protected final Map pendingRegistrationTasksMap;

	protected final NamespacePlugins namespacePlugins;

	protected final ApplicationEventMulticaster mcast;

	protected final ApplicationContextConfiguration config;

	protected Throwable creationTrace;

    protected Timer timer;

    protected Thread creationThread;

    protected ServiceDependentOsgiApplicationContext context;

    /**
	 * Find spring resources in the given bundle, and if an application context
	 * needs to be created, create it and add it to the map, keyed by bundle id
	 * 
	 * @param forBundle
	 * @param applicationContextMap
	 * @param pendingRegistrationTasks
	 */
	public ApplicationContextCreator(Bundle forBundle,
                                     Map applicationContextMap,
                                     Map contextsPendingInitializationMap,
			                         Map pendingRegistrationTasks,
                                     NamespacePlugins namespacePlugins,
                                     ApplicationContextConfiguration config,
			                         ApplicationEventMulticaster mcast,
                                     Timer timer) {
		this.bundle = forBundle;
		this.applicationContextMap = applicationContextMap;
		this.contextsPendingInitializationMap = contextsPendingInitializationMap;
		this.namespacePlugins = namespacePlugins;
		this.pendingRegistrationTasksMap = pendingRegistrationTasks;
		this.config = config;
		this.mcast = mcast;
        this.timer = timer;
        
        // Do some sanity checking.
		Assert.notNull(mcast);
		Long bundleKey = new Long(this.bundle.getBundleId());
		synchronized (pendingRegistrationTasksMap) {
			Assert.isTrue(!pendingRegistrationTasksMap.containsKey(bundleKey), "Duplicate context created!");
			pendingRegistrationTasksMap.put(bundleKey, this);
		}
		synchronized (this.applicationContextMap) {
			synchronized (this.contextsPendingInitializationMap) {
				Assert.isTrue(!contextsPendingInitializationMap.containsKey(bundleKey), "Duplicate context created!");
				Assert.isTrue(!applicationContextMap.containsKey(bundleKey), "Duplicate context created!");
			}
		}

		if (log.isInfoEnabled()) {
			creationTrace = new Throwable().fillInStackTrace();
		}
	}
 
	public void create(final TaskExecutor executor) {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		final Long bundleKey = new Long(this.bundle.getBundleId());

		if (!config.isSpringPoweredBundle()) {
			return;
		}

		synchronized (pendingRegistrationTasksMap) {
			if (pendingRegistrationTasksMap.remove(bundleKey) == null) {
				log.warn("Context creation aborted");
				return;
			}
		}

		postEvent(BundleEvent.STARTING);

		BundleContext bundleContext = OsgiBundleUtils.getBundleContext(bundle);
		if (bundleContext == null) {
			log.error("Could not start ApplicationContext from [" + config.getConfigurationLocations()[0]
					+ "]: failed to resolve BundleContext for bundle [" + bundle + "]");
			return;
		}
        if (log.isInfoEnabled()) {
            log.info("Starting bundle [" + bundle.getSymbolicName() + "] with configuration ["
                     + StringUtils.arrayToCommaDelimitedString(config.getConfigurationLocations()) + "]");
        }

        // The parent ClassLoader is this class so that imports present in
        // the extender will be honored.
        final ClassLoader cl = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(cl);
        LocalBundleContext.setContext(bundleContext);

        context = createApplicationContext(bundleContext, config.getConfigurationLocations());
        context.setPublishContextAsService(config.isPublishContextAsService());
        context.setNamespaceResolver(namespacePlugins);

        final TimerTask timeout = new TimerTask() {
            public void run() {
                context.interrupt();
                ApplicationContextException e = new ApplicationContextException(
                        "Application context initializition for '"
                        + bundle.getSymbolicName() + "' has timed out");
                e.fillInStackTrace();
                fail(e, bundleKey);
            }
        };

        synchronized (this.contextsPendingInitializationMap) {
            // creating the beans may take a long time (possible 'forever')
            // if the service dependencies are not satisfied. We need to be
            // able to stop this bundle and stop the context creation even
            // before it is fully completed initializing.
            this.contextsPendingInitializationMap.put(bundleKey, this);
        }

        long timeout_in_milliseconds = config.getTimeout() * 1000;
        timer.schedule(timeout, timeout_in_milliseconds); 

        final Runnable post = new Runnable() {
            public void run() {
                setThread(Thread.currentThread());
                timeout.cancel();
                ClassLoader ccl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(cl);
                try {
                    context.complete();
                    // ensure no-one else modifies the context map while we do this
                    // do not change locking order without also changing
                    // ApplicationContextCloser
                    synchronized (applicationContextMap) {
                        synchronized (contextsPendingInitializationMap) {
                            if (contextsPendingInitializationMap.containsKey(bundleKey)) {
                                // it is possible the key is no longer in the map if the
                                // bundle was stopped during the time it took us to get here...
                                contextsPendingInitializationMap.remove(bundleKey);
                                applicationContextMap.put(bundleKey, context);
                            }
                        }
                    }
                    postEvent(BundleEvent.STARTED);
                } catch (Throwable t) {
                    // ensure that the context class loader is set to our class loader,
                    // as we're now handling the failure in this context
                    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                    fail(t, bundleKey);
                } finally {
                    Thread.currentThread().setContextClassLoader(ccl);
                    setThread(null);
                }
            }
        };

        Runnable postAction;
        if (executor == null) {
            postAction = post;
        } else {
            postAction = new Runnable() {
                public void run() {
                    executor.execute(post);
                }
            };
        }

        try {
            setThread(Thread.currentThread());
            context.create(postAction);
		} catch (Throwable t) {
            timeout.cancel();
            fail(t, bundleKey);
		} finally {
			Thread.currentThread().setContextClassLoader(ccl);
		}
	}


    protected void fail(Throwable t,
                        Long bundleKey) {
        try {
            context.interrupt();

            // do not change locking order without also changing application
            // context closer
            synchronized (this.applicationContextMap) {
                synchronized (this.contextsPendingInitializationMap) {
                    this.contextsPendingInitializationMap.remove(bundleKey);
                    this.applicationContextMap.remove(bundleKey);
                }
            }
            postEvent(BundleEvent.STOPPED);

            StringBuffer buf = new StringBuffer();
            DependencyListener listener = context.getListener();
            if (listener == null || listener.getUnsatisfiedDependencies().isEmpty()) {
                buf.append("none");
            } else {
                for (Iterator dependencies = listener.getUnsatisfiedDependencies().iterator(); dependencies.hasNext();)
                {
                    Dependency dependency = (Dependency) dependencies.next();
                    buf.append(dependency.toString());
                    if (dependencies.hasNext()) {
                        buf.append(", ");
                    }
                }
            }

            if (log.isErrorEnabled()) {
                log.error("Unable to create application context for [" + bundle.getSymbolicName()
                          + "], unsatisfied dependencies: " + buf.toString(), t);
                if (log.isInfoEnabled()) {
                    log.info("[" + bundle.getSymbolicName() + "]" + " creation calling code: ", creationTrace);
                }
            }
        } catch (Throwable e) {
            t.printStackTrace();
            e.printStackTrace();
        }
    }


    protected ServiceDependentOsgiApplicationContext createApplicationContext(BundleContext context, String[] locations) {
		return new ServiceDependentOsgiBundleXmlApplicationContext(context, locations);
	}

	protected void postEvent(int starting) {
		mcast.multicastEvent(new SpringBundleEvent(starting, bundle));
	}

    protected ServiceDependentOsgiApplicationContext getContext() {
        return context;
    }

    // Used for testing until I can figure out something better.
    protected void setContext(ServiceDependentOsgiApplicationContext context) {
        this.context = context;
    }

    protected Thread getCreationThread() {
        return creationThread;
    }

    private void setThread(Thread creationThread) {
        this.creationThread = creationThread;
    }
}
