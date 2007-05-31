package org.springframework.osgi.extender.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.osgi.context.support.*;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Adrian Colyer
 * @author Hal Hildebrand
 */
public class ContextControl {

    protected volatile ContextLifecycle state = ContextLifecycle.PENDING;
    protected Bundle bundle;
    protected NamespacePlugins namespacePlugins;
    protected ApplicationEventMulticaster mcast;
    protected ApplicationContextConfiguration config;
    protected Throwable creationTrace;
    protected Timer timer;
    protected Thread creationThread;
    protected ServiceDependentOsgiApplicationContext context;

    private static final Log log = LogFactory.getLog(ContextControl.class);


    public void create(final TaskExecutor executor) {
        if (state != ContextLifecycle.PENDING) {
            log.warn("Context creation aborted, lifecyle state: " + state);
            return;
        }

        state = ContextLifecycle.CREATING;
        postEvent(BundleEvent.STARTING);

        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        BundleContext bundleContext = OsgiBundleUtils.getBundleContext(bundle);
        if (bundleContext == null) {
            log.error("Could not start ApplicationContext from [" + config.getConfigurationLocations()[0]
                      + "]: failed to resolve BundleContext for bundle [" + bundle + "]");
            state = ContextLifecycle.DESTROYED;
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Starting bundle [" + bundle.getSymbolicName() + "] with configuration ["
                     + StringUtils.arrayToCommaDelimitedString(config.getConfigurationLocations()) + "]");
        }

        // The parent ClassLoader is this class so that imports present in
        // the extender will be honored.
        final ClassLoader cl = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle,
                                                                                      getClass().getClassLoader());
        Thread.currentThread().setContextClassLoader(cl);
        LocalBundleContext.setContext(bundleContext);

        context = createApplicationContext(bundleContext, config.getConfigurationLocations());
        context.setPublishContextAsService(config.isPublishContextAsService());
        context.setNamespaceResolver(namespacePlugins);

        final TimerTask timeout = new TimerTask() {
            public void run() {
                ApplicationContextException e = new ApplicationContextException(
                        "Application context initializition for '"
                        + bundle.getSymbolicName() + "' has timed out");
                e.fillInStackTrace();
                fail(e);
            }
        };

        long timeout_in_milliseconds = config.getTimeout() * 1000;
        timer.schedule(timeout, timeout_in_milliseconds);

        Runnable postAction;
        if (executor == null) {
            postAction = postAction(timeout, cl);
        } else {
            postAction = new Runnable() {
                public void run() {
                    executor.execute(postAction(timeout, cl));
                }
            };
        }

        try {
            setThread(Thread.currentThread());
            context.create(postAction);
        } catch (ThreadDeath td) {
            Thread.currentThread().setContextClassLoader(ccl);
            if (log.isDebugEnabled()) {
                log.debug("Context creation interrupted for [" + bundle.getSymbolicName() + "]", td);
            }
        } catch (Throwable t) {
            fail(t);
        } finally {
            timeout.cancel();
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }


    public void close() {
        if (state == ContextLifecycle.PENDING) {
            state = ContextLifecycle.DESTROYED;
            return;
        }

        // Check for tasks that have not closed yet

        if (state == ContextLifecycle.CREATING) {
            state = ContextLifecycle.DESTROYING;
            if (log.isInfoEnabled()) {
                log.info("Closing application context which is in creation process for bundle [" +
                         bundle.getSymbolicName() + "]");
            }
            interrupt();
        } else if (state == ContextLifecycle.CREATED) {
            state = ContextLifecycle.DESTROYING;
            postEvent(BundleEvent.STOPPING);
            if (log.isInfoEnabled()) {
                log.info("Closing application context for bundle [" +
                         bundle.getSymbolicName() + "]");
            }
            context.close();
            state = ContextLifecycle.DESTROYED;
            postEvent(BundleEvent.STOPPED);
        } else {
            // Context is being destroyed.  NoOp.
        }
    }


    protected void fail(Throwable t) {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            interrupt();

            StringBuffer buf = new StringBuffer();
            DependencyListener listener = context.getListener();
            if (listener == null || listener.getUnsatisfiedDependencies().isEmpty()) {
                buf.append("none");
            } else {
                for (Iterator dependencies = listener.getUnsatisfiedDependencies().iterator();
                     dependencies.hasNext();) {
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
            // last ditch effort to get useful error information
            t.printStackTrace();
            e.printStackTrace();
        }
    }


    protected Runnable postAction(final TimerTask timeout, final ClassLoader cl) {
        return new Runnable() {
            public void run() {
                setThread(Thread.currentThread());
                timeout.cancel();
                ClassLoader ccl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(cl);
                try {
                    context.complete();
                    if (context.getState() == ContextState.CREATED) {
                        state = ContextLifecycle.CREATED;
                        postEvent(BundleEvent.STARTED);
                    }
                } catch (ThreadDeath td) {
                    if (log.isDebugEnabled()) {
                        log.debug("Context creation interrupted for [" + bundle.getSymbolicName() + "]", td);
                    }
                } catch (Throwable t) {
                    fail(t);
                } finally {
                    Thread.currentThread().setContextClassLoader(ccl);
                    setThread(null);
                }
            }
        };
    }


    protected void interrupt() {
        if (context == null) {
            return; // nothing to do
        }
        state = ContextLifecycle.DESTROYING;
        postEvent(BundleEvent.STOPPING);
        context.interrupt();
        if (creationThread != null) {
            creationThread.interrupt();
            try {
                //noinspection deprecation
                creationThread.stop();
            } catch (ThreadDeath e) {
                // ignore if this is the current thread.
            }
            creationThread = null;
        }
        context.close();
        state = ContextLifecycle.DESTROYED;
        postEvent(BundleEvent.STOPPED);
    }


    protected ServiceDependentOsgiApplicationContext createApplicationContext(BundleContext context,
                                                                              String[] locations) {
        return new ServiceDependentOsgiBundleXmlApplicationContext(context, locations);
    }


    protected void postEvent(int starting) {
        mcast.multicastEvent(new SpringBundleEvent(starting, bundle));
    }


    // Used for testing
    protected void setContext(ServiceDependentOsgiApplicationContext context) {
        this.context = context;
    }


    protected ServiceDependentOsgiApplicationContext getContext() {
        return context;
    }


    private void setThread(Thread creationThread) {
        this.creationThread = creationThread;
    }


    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }


    public Bundle getBundle() {
        return bundle;
    }


    public void setConfig(ApplicationContextConfiguration config) {
        this.config = config;
    }


    public void setMcast(ApplicationEventMulticaster mcast) {
        this.mcast = mcast;
    }


    public void setNamespacePlugins(NamespacePlugins namespacePlugins) {
        this.namespacePlugins = namespacePlugins;
    }


    public void setTimer(Timer timer) {
        this.timer = timer;
    }


    public ContextLifecycle getState() {
        return state;
    }


    // for testing only!
    protected void setState(ContextLifecycle state) {
        this.state = state;
    }
}
