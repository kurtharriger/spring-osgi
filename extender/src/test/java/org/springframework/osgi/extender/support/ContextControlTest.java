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
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.osgi.context.support.ApplicationContextConfiguration;
import org.springframework.osgi.context.support.SpringBundleEvent;
import org.springframework.osgi.mock.EntryLookupControllingMockBundle;
import org.springframework.osgi.mock.MockBundle;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Timer;

/**
 * @author Adrian Colyer
 * @author Hal Hildebrand
 */
public class ContextControlTest extends TestCase {

    private static final String[] META_INF_SPRING_CONTENT = new String[]{"file://META-INF/spring/context.xml",
                                                                         "file://META-INF/spring/context-two.xml"};

    private ContextControl contextControl;
    private final ApplicationEventMulticaster mcast = new SimpleApplicationEventMulticaster();
    private final Bundle bundle = new MockBundle();
    private final Timer timer = new Timer(true);


    public void testCloseWithNoContexts() {
        contextControl = new ContextControl();
        contextControl.setBundle(bundle);
        contextControl.setMcast(mcast);
        MockControl control = MockControl.createControl(ServiceDependentOsgiApplicationContext.class);
        ServiceDependentOsgiApplicationContext mockContext =
                (ServiceDependentOsgiApplicationContext) control.getMock();
        contextControl.setContext(mockContext);
        control.replay();

        contextControl.close();
        assertEquals("state DESTROYED", ContextLifecycle.DESTROYED, contextControl.getState());
    }


    public void testCloseWithFullContext() {
        contextControl = new ContextControl();
        contextControl.setBundle(bundle);
        contextControl.setMcast(mcast);
        MockControl control = MockControl.createControl(ServiceDependentOsgiApplicationContext.class);
        ServiceDependentOsgiApplicationContext mockContext =
                (ServiceDependentOsgiApplicationContext) control.getMock();
        mockContext.close();
        control.replay();
        contextControl.setState(ContextLifecycle.CREATED);
        contextControl.setContext(mockContext);
        contextControl.close();
        control.verify();
        assertEquals("state DESTROYED", ContextLifecycle.DESTROYED, contextControl.getState());
    }


    public void testCloseWithInitializingContext() {
        contextControl = new ContextControl();
        contextControl.setBundle(bundle);
        contextControl.setMcast(mcast);
        MockControl control = MockControl.createControl(ServiceDependentOsgiApplicationContext.class);
        ServiceDependentOsgiApplicationContext mockContext =
                (ServiceDependentOsgiApplicationContext) control.getMock();
        mockContext.interrupt();
        mockContext.close();
        control.replay();
        contextControl.setContext(mockContext);
        contextControl.setState(ContextLifecycle.CREATING);
        contextControl.close();
        control.verify();
    }


    public void testCloseEvent() {
        contextControl = new ContextControl();
        contextControl.setBundle(bundle);
        contextControl.setMcast(mcast);
        MockControl control = MockControl.createControl(ServiceDependentOsgiApplicationContext.class);
        ServiceDependentOsgiApplicationContext mockContext =
                (ServiceDependentOsgiApplicationContext) control.getMock();
        mockContext.close();
        MockControl mockListener = MockControl.createControl(ApplicationListener.class);
        ApplicationListener listener = (ApplicationListener) mockListener.getMock();
        mcast.addApplicationListener(listener);

        control.replay();
        contextControl.setContext(mockContext);
        contextControl.setState(ContextLifecycle.CREATED);
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPING, bundle));
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPED, bundle));
        mockListener.replay();

        contextControl.close();
        control.verify();
        mockListener.verify();
        mcast.removeAllListeners();
    }


    public void testNoContextCreatedIfNotSpringPowered() {
        EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
        aBundle.setResultsToReturnOnNextCallToFindEntries(null);
        createContextControl(aBundle).create(null); // will NPE if not detecting that this
        // bundle is
        // not
        // spring-powered!
    }


    public void testContextIsPlacedIntoPendingMapPriorToRefreshAndMovedAfterwards() {
        EntryLookupControllingMockBundle aBundle = new RepeatingEntryLookupControllingMockBundle(null);
        aBundle.setResultsToReturnOnNextCallToFindEntries(META_INF_SPRING_CONTENT);
        final MapTestingBundleXmlApplicationContext testingContext = new MapTestingBundleXmlApplicationContext(
                aBundle.getContext(), META_INF_SPRING_CONTENT);

        contextControl = new ContextControl() {

            protected ServiceDependentOsgiApplicationContext createApplicationContext(BundleContext context,
                                                                                      String[] locations) {
                return testingContext;
            }
        };
        contextControl.setBundle(aBundle);
        contextControl.setTimer(timer);
        contextControl.setMcast(mcast);
        contextControl.setConfig(new ApplicationContextConfiguration(aBundle));
        contextControl.setContext(testingContext);

        contextControl.create(null);

        assertTrue("context refreshed", testingContext.isRefreshed);

        assertEquals("Context Lifecycle = CREATED", ContextLifecycle.CREATED, contextControl.getState());
    }


    public void testContextIsRemovedFromMapsOnException() {
        EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
        aBundle.setResultsToReturnOnNextCallToFindEntries(META_INF_SPRING_CONTENT);
        final ServiceDependentOsgiBundleXmlApplicationContext testContext
                = new ServiceDependentOsgiBundleXmlApplicationContext(aBundle.getContext(),
                                                                      META_INF_SPRING_CONTENT) {
            public void create(Runnable postAction) {
                throw new RuntimeException("bang! (this exception deliberately caused by test case)") {
                    public synchronized Throwable fillInStackTrace() {
                        return null;
                    }
                };
            }
        };
        contextControl = new ContextControl() {

            protected ServiceDependentOsgiApplicationContext createApplicationContext(BundleContext context,
                                                                                      String[] locations) {
                return testContext;
            }

        };
        contextControl.setBundle(aBundle);
        contextControl.setTimer(timer);
        contextControl.setMcast(mcast);
        contextControl.setConfig(new ApplicationContextConfiguration(aBundle));
        contextControl.setContext(testContext);

        try {
            contextControl.create(null);
        }
        catch (Throwable t) {
            fail("Exception should have been handled inside control");
        }

        assertEquals("Context Lifecycle = DESTROYED", ContextLifecycle.DESTROYED, contextControl.getState());
        assertEquals("Context State = CLOSED", ContextState.CLOSED, contextControl.getContext().getState());
    }


    public void testCreationEvent() {
        EntryLookupControllingMockBundle aBundle = new EntryLookupControllingMockBundle(null);
        aBundle.setResultsToReturnOnNextCallToFindEntries(META_INF_SPRING_CONTENT);
        MockControl mockListener = MockControl.createControl(ApplicationListener.class);
        ApplicationListener listener = (ApplicationListener) mockListener.getMock();
        mcast.addApplicationListener(listener);
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STARTING, aBundle));
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STARTED, aBundle));
        mockListener.replay();

        createContextControl(aBundle).create(null);

        // mockListener.verify();
        mcast.removeAllListeners();
    }


    public void testCreationFailureEvent() {
        EntryLookupControllingMockBundle aBundle = new RepeatingEntryLookupControllingMockBundle(null);
        aBundle.setResultsToReturnOnNextCallToFindEntries(META_INF_SPRING_CONTENT);
        MockControl mockListener = MockControl.createControl(ApplicationListener.class);
        ApplicationListener listener = (ApplicationListener) mockListener.getMock();
        mcast.addApplicationListener(listener);
        final ServiceDependentOsgiBundleXmlApplicationContext testContext
                = new ServiceDependentOsgiBundleXmlApplicationContext(aBundle.getContext(),
                                                                      META_INF_SPRING_CONTENT) {
            public void create(Runnable postAction) {
                throw new RuntimeException("bang! (this exception deliberately caused by test case)") {
                    public synchronized Throwable fillInStackTrace() {
                        return null;
                    }
                };
            }
        };
        contextControl = new ContextControl() {

            protected ServiceDependentOsgiApplicationContext createApplicationContext(BundleContext context,
                                                                                      String[] locations) {
                return testContext;
            }

        };
        contextControl.setBundle(aBundle);
        contextControl.setTimer(timer);
        contextControl.setMcast(mcast);
        contextControl.setConfig(new ApplicationContextConfiguration(aBundle));
        contextControl.setContext(testContext);

        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STARTING, aBundle));
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPING, aBundle));
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPED, aBundle));
        mockListener.replay();

        contextControl.create(null);

        mockListener.verify();
        mcast.removeAllListeners();
    }


    private ContextControl createContextControl(Bundle aBundle) {
        ContextControl control = new ContextControl() {
            protected ServiceDependentOsgiApplicationContext createApplicationContext(BundleContext context,
                                                                                      String[] locations) {
                return new ServiceDependentOsgiBundleXmlApplicationContext(context, locations) {
                    public void create(Runnable postAction) throws BeansException {
                        postAction.run();
                    }


                    protected void postRefresh() {
                    }
                };
            }
        };

        control.setBundle(aBundle);
        control.setTimer(timer);
        control.setMcast(mcast);
        control.setConfig(new ApplicationContextConfiguration(aBundle));

        return control;

    }


    private class MapTestingBundleXmlApplicationContext extends ServiceDependentOsgiBundleXmlApplicationContext {

        public boolean isRefreshed = false;


        public MapTestingBundleXmlApplicationContext(BundleContext context, String[] configLocations) {
            super(context, configLocations);
        }


        public void create(Runnable postAction) {
            assertEquals("Control state == CREATING", ContextLifecycle.CREATING, contextControl.getState());
            isRefreshed = true;
            postAction.run();
        }


        protected void postRefresh() {
        }
    }

    private static class RepeatingEntryLookupControllingMockBundle extends EntryLookupControllingMockBundle {
        protected String[] findResult;


        public RepeatingEntryLookupControllingMockBundle(Dictionary headers) {
            super(headers);
        }


        public Enumeration findEntries(String path, String filePattern, boolean recurse) {
            if (this.nextFindResult == null) {
                return super.findEntries(path, filePattern, recurse);
            } else {
                Enumeration r = this.nextFindResult;
                this.nextFindResult = createEnumerationOver(findResult);
                return r;
            }
        }


        public void setResultsToReturnOnNextCallToFindEntries(String[] r) {
            findResult = r;
            if (findResult == null) {
                findResult = new String[0];
            }
            this.nextFindResult = createEnumerationOver(findResult);
        }
    }

}
