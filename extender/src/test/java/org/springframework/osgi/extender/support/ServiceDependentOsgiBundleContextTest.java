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

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.TaskExecutor;
import org.springframework.osgi.context.support.SpringBundleEvent;
import org.springframework.osgi.mock.MockBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 * @author Hal Hildebrand
 */
public class ServiceDependentOsgiBundleContextTest extends TestCase {

    private static final String[] META_INF_SPRING_CONTENT = new String[]{"file://META-INF/spring/context.xml",
                                                                         "file://META-INF/spring/context-two.xml"};

    private MockServiceDependentOsgiBundleXmlApplicationContext context;
    private Bundle bundle = new MockBundle();

    private ApplicationEventMulticaster mcast = new SimpleApplicationEventMulticaster();
    private Timer timer = new Timer(true);


    protected void setUp() {
        context = new MockServiceDependentOsgiBundleXmlApplicationContext(META_INF_SPRING_CONTENT);
        context.setMcast(mcast);
        context.setTimer(timer);
        context.setTimeout(300);
    }


    public void testFullCycle() {
        context.refresh();
        context.close();
        assertEquals("state CLOSED", ContextState.CLOSED, context.getState());
        assertFalse("listener not registered", context.listener.registered);
        assertFalse("listener not deregistered", context.listener.deregistered);
        assertTrue("preRefresh performed", context.preRefreshDone);
        assertTrue("onRefresh performed", context.onRefreshDone);
        assertTrue("postRefresh performed", context.postRefreshDone);
        List transitions = context.stateTransitions;
        assertEquals("number of state transitions", 5, transitions.size());
        assertEquals("INITIALIZED", ContextState.INITIALIZED, transitions.get(0));
        assertEquals("RESOLVING_DEPENDENCIES", ContextState.RESOLVING_DEPENDENCIES, transitions.get(1));
        assertEquals("DEPENDENCIES_RESOLVED", ContextState.DEPENDENCIES_RESOLVED, transitions.get(2));
        assertEquals("CREATED", ContextState.CREATED, transitions.get(3));
        assertEquals("CLOSED", ContextState.CLOSED, transitions.get(4));
    }


    public void testExceptionInPreRefresh() {
        context.exceptionInPreRefresh = new TestException("expected!");
        context.refresh();
        assertEquals("state CLOSED", ContextState.CLOSED, context.getState());
        assertFalse("listener not registered", context.listener.registered);
        assertFalse("listener not deregistered", context.listener.deregistered);
        assertFalse("preRefresh not performed", context.preRefreshDone);
        assertFalse("onRefresh not performed", context.onRefreshDone);
        assertFalse("postRefresh not performed", context.postRefreshDone);
        List transitions = context.stateTransitions;
        assertEquals("number of state transitions", 3, transitions.size());
        assertEquals("INITIALIZED", ContextState.INITIALIZED, transitions.get(0));
        assertEquals("INTERRUPTED", ContextState.INTERRUPTED, transitions.get(1));
        assertEquals("CLOSED", ContextState.CLOSED, transitions.get(2));
    }


    public void testExceptionInPostRefresh() {
        context.exceptionInPostRefresh = new TestException("expected!");
        context.refresh();
        assertEquals("state CLOSED", ContextState.CLOSED, context.getState());
        assertFalse("listener not registered", context.listener.registered);
        assertFalse("listener not deregistered", context.listener.deregistered);
        assertTrue("preRefresh performed", context.preRefreshDone);
        assertTrue("onRefresh performed", context.onRefreshDone);
        assertFalse("postRefresh not performed", context.postRefreshDone);
        List transitions = context.stateTransitions;
        assertEquals("number of state transitions", 5, transitions.size());
        assertEquals("INITIALIZED", ContextState.INITIALIZED, transitions.get(0));
        assertEquals("RESOLVING_DEPENDENCIES", ContextState.RESOLVING_DEPENDENCIES, transitions.get(1));
        assertEquals("DEPENDENCIES_RESOLVED", ContextState.DEPENDENCIES_RESOLVED, transitions.get(2));
        assertEquals("INTERRUPTED", ContextState.INTERRUPTED, transitions.get(3));
        assertEquals("CLOSED", ContextState.CLOSED, transitions.get(4));
        assertSame("Correct TCCL on failure", getClass().getClassLoader(), context.loaderOnFailure);
    }


    public void testMulticastEvents() {
        MockControl mockListener = MockControl.createControl(ApplicationListener.class);
        ApplicationListener listener = (ApplicationListener) mockListener.getMock();
        mcast.addApplicationListener(listener);
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STARTING, bundle));
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STARTED, bundle));
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPING, bundle));
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPED, bundle));
        mockListener.replay();
        context.refresh();
        context.close();
        mockListener.verify();
        mcast.removeAllListeners();
    }


    public void testClassLoader() {
        ClassLoader testLoader = new TestLoader();
        context.classLoader = testLoader;
        context.refresh();
        assertSame("TCCL preRefresh", testLoader, context.loaderOnPreRefresh);
        assertSame("TCCL onRefresh", testLoader, context.loaderOnRefresh);
        assertSame("TCCL postRefresh", testLoader, context.loaderOnPostRefresh);
    }


    public void testAsynchCreation() {
        MockTaskExecutor executor = new MockTaskExecutor();
        context.setExecutor(executor);

        ClassLoader testLoader = new TestLoader();
        context.classLoader = testLoader;
        context.listener.hasDependencies = true;

        context.refresh();
        assertTrue("listener registered", context.listener.registered);
        assertFalse("listener not deregistered", context.listener.deregistered);
        List transitions = context.stateTransitions;
        assertEquals("number of state transitions", 2, transitions.size());
        assertEquals("INITIALIZED", ContextState.INITIALIZED, transitions.get(0));
        assertEquals("RESOLVING_DEPENDENCIES", ContextState.RESOLVING_DEPENDENCIES, transitions.get(1));

        context.dependenciesAreSatisfied();

        transitions = context.stateTransitions;
        assertEquals("number of state transitions", 4, transitions.size());
        assertEquals("INITIALIZED", ContextState.INITIALIZED, transitions.get(0));
        assertEquals("RESOLVING_DEPENDENCIES", ContextState.RESOLVING_DEPENDENCIES, transitions.get(1));
        assertEquals("DEPENDENCIES_RESOLVED", ContextState.DEPENDENCIES_RESOLVED, transitions.get(2));
        assertEquals("CREATED", ContextState.CREATED, transitions.get(3));


        context.close();
        assertTrue("listener deregistered", context.listener.deregistered);

        transitions = context.stateTransitions;
        assertEquals("number of state transitions", 5, transitions.size());
        assertEquals("INITIALIZED", ContextState.INITIALIZED, transitions.get(0));
        assertEquals("RESOLVING_DEPENDENCIES", ContextState.RESOLVING_DEPENDENCIES, transitions.get(1));
        assertEquals("DEPENDENCIES_RESOLVED", ContextState.DEPENDENCIES_RESOLVED, transitions.get(2));
        assertEquals("CREATED", ContextState.CREATED, transitions.get(3));
        assertEquals("CLOSED", ContextState.CLOSED, transitions.get(4));

        assertSame("TCCL preRefresh", testLoader, context.loaderOnPreRefresh);
        assertSame("TCCL onRefresh", testLoader, context.loaderOnRefresh);
        assertSame("TCCL postRefresh", testLoader, context.loaderOnPostRefresh);

        assertEquals("executed once", 1, executor.executionCount);

    }


    private class MockServiceDependentOsgiBundleXmlApplicationContext
            extends ServiceDependentOsgiBundleXmlApplicationContext {
        public List stateTransitions = new ArrayList();
        public boolean preRefreshDone = false;
        public boolean postRefreshDone = false;
        public boolean onRefreshDone = false;
        public ClassLoader classLoader = getClass().getClassLoader();
        public ClassLoader loaderOnRefresh;
        public ClassLoader loaderOnPostRefresh;
        public ClassLoader loaderOnPreRefresh;
        public ClassLoader loaderOnFailure;
        public RuntimeException exceptionInPreRefresh;
        public RuntimeException exceptionInPostRefresh;
        public MockDependencyListener listener = new MockDependencyListener(this);


        public List getStateTransitions() {
            return stateTransitions;
        }


        protected void setState(ContextState state) {
            if (state == stateTransitions.get(stateTransitions.size() - 1)) {
                Exception e = new Exception("Duplicate state!");
                e.fillInStackTrace();
                e.printStackTrace();
            }
            stateTransitions.add(state);
            super.setState(state);
        }


        public MockServiceDependentOsgiBundleXmlApplicationContext(String[] configLocations) {
            super(configLocations);
            stateTransitions.add(ContextState.INITIALIZED);
        }


        public Bundle getBundle() {
            return bundle;
        }


        public ClassLoader getClassLoader() {
            return classLoader;
        }


        protected void preRefresh() {
            loaderOnPreRefresh = Thread.currentThread().getContextClassLoader();
            refreshBeanFactory();
            if (exceptionInPreRefresh != null) {
                throw exceptionInPreRefresh;
            }
            preRefreshDone = true;
        }


        protected void postRefresh() {
            loaderOnPostRefresh = Thread.currentThread().getContextClassLoader();
            if (exceptionInPostRefresh != null) {
                throw exceptionInPostRefresh;
            }
            postRefreshDone = true;
        }


        protected void onRefresh() {
            loaderOnRefresh = Thread.currentThread().getContextClassLoader();
            onRefreshDone = true;
        }


        protected void fail(Throwable t) {
            super.fail(t);
            loaderOnFailure = Thread.currentThread().getContextClassLoader();
        }


        protected DefaultListableBeanFactory createBeanFactory() {
            return new DefaultListableBeanFactory();
        }


        protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {

        }


        public void publishEvent(ApplicationEvent event) {

        }


        protected DependencyListener createDependencyListener() {
            return listener;
        }
    }

    private static class MockDependencyListener extends DependencyListener {
        public boolean hasDependencies = false;
        public boolean registered = false;
        public boolean deregistered = false;


        public MockDependencyListener(ServiceDependentOsgiBundleXmlApplicationContext context) {
            super(context);
        }


        protected boolean isSatisfied() {
            return !hasDependencies;
        }


        protected void register() {
            registered = true;
        }


        protected void deregister() {
            deregistered = true;
        }
    }

    private static class TestLoader extends ClassLoader {

    }

    private static class TestException extends RuntimeException {

        public TestException(String message, Throwable cause) {
            super(message, cause);
        }


        public TestException(String message) {
            super(message);
        }


        public TestException(Throwable cause) {
            super(cause);
        }


        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }

    private static class MockTaskExecutor implements TaskExecutor {
        public int executionCount = 0;


        public void execute(Runnable task) {
            executionCount++;
            task.run();
        }
    }
}
