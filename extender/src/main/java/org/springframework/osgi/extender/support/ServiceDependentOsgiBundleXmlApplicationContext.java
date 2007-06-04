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

import org.osgi.framework.BundleEvent;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.context.support.SpringBundleEvent;
import org.springframework.core.task.TaskExecutor;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Timer;
import java.util.Iterator;
import java.util.TimerTask;

/**
 * An OsgiBundleXmlApplicationContext which delays initialization until all
 * of its osgi:reference targets are complete. <p/>
 *
 * @author Andy Piper
 * @author Hal Hildebrand
 * @author Costin Leau
 */
public class ServiceDependentOsgiBundleXmlApplicationContext extends OsgiBundleXmlApplicationContext
        implements ServiceDependentOsgiApplicationContext {
    private volatile ContextState state = ContextState.INITIALIZED;
    protected DependencyListener listener;
    protected TaskExecutor executor;
    protected Timer timer;
    protected Throwable creationTrace;
    protected Thread creationThread;
    protected long timeout;
    protected TimerTask timerTask;
    protected ApplicationEventMulticaster mcast;

    private static final Log log = LogFactory.getLog(ServiceDependentOsgiBundleXmlApplicationContext.class);


    public ServiceDependentOsgiBundleXmlApplicationContext(String[] configLocations) {
        super(configLocations);
    }


    /**
     * Refresh the context.  This is the heart of darkness as this is designed to be an asynchronous process.
     * When this method returns, the refresh process may be incomplete as there could be services that this
     * context is dependendent upon which are unresolved.
     */
    public synchronized void refresh() {
        if (getState() == ContextState.INTERRUPTED) {
            logger.warn("Context creation has been interrupted: " + getDisplayName());
            return;
        }

        creationTrace = new Exception("Context Refresh Trace");
        creationTrace.fillInStackTrace();

        scheduleTimeout();

        postEvent(BundleEvent.STARTING);

        creationThread = Thread.currentThread();
        ClassLoader ccl = creationThread.getContextClassLoader();
        creationThread.setContextClassLoader(getClassLoader());
        try {
            preRefresh();
            DependencyListener dl = createDependencyListener();
            dl.findServiceDependencies();

            if (getState() == ContextState.INTERRUPTED) {
                logger.warn("Context creation has been interrupted: " + getDisplayName());
                return;
            }

            setState(ContextState.RESOLVING_DEPENDENCIES);
            if (!dl.isSatisfied()) {
                // Asynchronously finish the context refresh process
                listener = dl;
                listener.register();
            } else {
                // Synchronously finish the context refresh process
                if (getState() == ContextState.INTERRUPTED) {
                    logger.warn("Context creation has been interrupted: " + getDisplayName());
                    return;
                } 
                if (logger.isDebugEnabled()) {
                    logger.debug("No outstanding dependencies, completing initialization for "
                                 + getDisplayName());
                }
                dependenciesAreSatisfied(false);
                creationThread = null;
            }
        } catch (Throwable e) {
            fail(e);  
        } finally {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }


    public synchronized void close() {
        if (getState() == ContextState.CLOSED) {
            return;
        }
        try {
            postEvent(BundleEvent.STOPPING);
            if (getState() != ContextState.CREATED && getState() != ContextState.INTERRUPTED) {
                interrupt();
            } else {
                if (listener != null) {
                    listener.deregister();
                }
                super.close();
                setState(ContextState.CLOSED);
                postEvent(BundleEvent.STOPPED);
            }
        } finally {
            creationTrace = null;
            creationThread = null;
        }
    }
    

    protected void postRefresh() {
        if (getState() == ContextState.INTERRUPTED) {
            logger.warn("Context creation has been interrupted: " + getDisplayName());
            return;
        }
        super.postRefresh();
        setState(ContextState.CREATED);
        postEvent(BundleEvent.STARTED);
    }


    protected DependencyListener createDependencyListener() {
        return new DependencyListener(this);
    }


    /**
     * The service dependencies of the receiver are satisfied; continue with the creation
     * of the context.
     * @param spawn - if true, then spawn a new thread if the executor is available
     */
    protected void dependenciesAreSatisfied(boolean spawn) {
        setState(ContextState.DEPENDENCIES_RESOLVED);
        if (!spawn || executor == null) {
            // execute synchronously in the same thread
            postAction().run();
        } else {
            // execute asynchronously
            executor.execute(postAction());
        }
    }


    /**
     * Schedule the timer task which will terminate the context creation process if dependencies are not
     * satisfied within the specified timeout.
     */
    protected void scheduleTimeout() {
        timerTask = new TimerTask() {
            public void run() {
                ApplicationContextException e =
                        new ApplicationContextException("Application context initializition for '" +
                                                        getBundleSymbolicName() +
                                                        "' has timed out");
                e.fillInStackTrace();
                fail(e);
            }
        };

        long timeout_in_milliseconds = timeout * 1000;
        timer.schedule(timerTask, timeout_in_milliseconds);
    }


    /**
     * Crete the Runnable action which will complete the context creation process.
     * This process can be called synchronously or asynchronously, depending on context
     * configuration and availability of dependencies.  Therefore, it is super critical
     * to have the correct context class loader set as well as the correct error handling.
     * @return
     */
    protected Runnable postAction() {
        return new Runnable() {
            public void run() {
                creationThread = Thread.currentThread();
                timerTask.cancel();
                ClassLoader ccl = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getClassLoader());
                try {
                    // Continue with the refresh process...
                    synchronized (this) {
                        onRefresh();
                        postRefresh();
                        setState(ContextState.CREATED);  
                    }
                    postEvent(BundleEvent.STARTED);
                } catch (ThreadDeath td) {
                    if (log.isDebugEnabled()) {
                        log.debug("Context creation interrupted for [" + getDisplayName() + "]", td);
                    }
                } catch (Throwable t) {
                    fail(t);
                } finally {
                    Thread.currentThread().setContextClassLoader(ccl);
                    creationThread = null;
                    creationTrace = null;
                }
            }
        };
    }


    /**
     * Interrupt the context creation proces.  Deregister the listeners, interrupt the creation thread
     * and close the receiver.
     */
    protected void interrupt() {
        if (getState() == ContextState.INTERRUPTED || getState() == ContextState.CLOSED) {
            return;
        }
        setState(ContextState.INTERRUPTED);
        if (listener != null) {
            listener.deregister();
        }
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
        close();
    }


    /**
     * Fail creating the context.  Figure out unsatisfied dependencies and provide a very nice log message.
     * @param t - the offending Throwable which caused our demise
     */
    protected void fail(Throwable t) {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            interrupt();

            StringBuffer buf = new StringBuffer();
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
                log.error("Unable to create application context for [" +
                          getBundleSymbolicName() +
                          "], unsatisfied dependencies: " +
                          buf.toString(), t);
                if (log.isInfoEnabled()) {
                    log.info("[" +
                             getBundleSymbolicName() +
                             "]" +
                             " creation calling code: ", creationTrace);
                }
            }
        } catch (Throwable e) {
            // last ditch effort to get useful error information
            t.printStackTrace();
            e.printStackTrace();
        } finally {
            creationThread = null;
            creationTrace = null;
        }
    }


    protected void postEvent(int starting) {
        mcast.multicastEvent(new SpringBundleEvent(starting, getBundle()));
    }


    public boolean isAvailable() {
        return isActive();
    }

    public boolean isActive() {
         return getState() == ContextState.CREATED;
    }


    public void setExecutor(TaskExecutor executor) {
        this.executor = executor;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }


    public void setMcast(ApplicationEventMulticaster mcast) {
        this.mcast = mcast;
    }

    public ContextState getState() {
        return state;
    }

    protected void setState(ContextState state) {
        this.state = state;
    }
}
