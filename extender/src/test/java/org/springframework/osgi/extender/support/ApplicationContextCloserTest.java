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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.springframework.context.ApplicationListener; 
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.context.support.SpringBundleEvent;

/**
 * @author Adrian Colyer
 */
public class ApplicationContextCloserTest extends TestCase {

	private ApplicationContextCloser closer;
    private ApplicationContextCreator creator;
    private Map initMap;
	private Map contextMap;
	private final ApplicationEventMulticaster mcast = new SimpleApplicationEventMulticaster();
	private final Bundle bundle = new MockBundle();
	private Map pendingRegistrationTasks;

	protected void setUp() throws Exception {
		this.contextMap = new HashMap();
		this.initMap = new HashMap();
        this.pendingRegistrationTasks = new HashMap();
        this.closer = new ApplicationContextCloser(bundle, contextMap, initMap, pendingRegistrationTasks, mcast);
        this.creator = new ApplicationContextCreator(bundle, contextMap, initMap, pendingRegistrationTasks, null, null, mcast, null);

        this.pendingRegistrationTasks.clear();
        super.setUp();
	}

	public void testCloseWithNoContexts() {
		this.closer.close();
		// nothing we can really verify :(
	}

	public void testCloseWithFullContext() {
		MockControl control = MockControl.createControl(ServiceDependentOsgiApplicationContext.class);
		ServiceDependentOsgiApplicationContext mockContext =
                (ServiceDependentOsgiApplicationContext) control.getMock();
        mockContext.getState();
        control.setReturnValue(ContextState.CREATED);
		mockContext.close();
        control.replay(); 
        this.contextMap.put(new Long(0), mockContext);
        this.closer.close();
		control.verify();
	}

	public void testCloseWithInitializingContext() {
		MockControl control = MockControl.createControl(ServiceDependentOsgiApplicationContext.class);
		ServiceDependentOsgiApplicationContext mockContext =
                (ServiceDependentOsgiApplicationContext) control.getMock();
        mockContext.getState();
        control.setReturnValue(ContextState.CREATED);
		mockContext.interrupt();
        mockContext.close();
        control.replay();
        creator.setContext(mockContext);
        this.initMap.put(new Long(0), creator);
		this.closer.close();
		control.verify();
	}

	public void testCloseEvent() {
		MockControl control = MockControl.createControl(ServiceDependentOsgiApplicationContext.class);
		ServiceDependentOsgiApplicationContext mockContext =
                (ServiceDependentOsgiApplicationContext) control.getMock();
        mockContext.getState();
        control.setReturnValue(ContextState.CREATED);
        mockContext.interrupt();
        mockContext.close();
		MockControl mockListener = MockControl.createControl(ApplicationListener.class);
		ApplicationListener listener = (ApplicationListener) mockListener.getMock();
		mcast.addApplicationListener(listener);

		control.replay();
        creator.setContext(mockContext);
        listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPING, bundle));
		listener.onApplicationEvent(new SpringBundleEvent(BundleEvent.STOPPED, bundle));
		mockListener.replay();

		this.initMap.put(new Long(0), creator);
		this.closer.close();
		control.verify();
		mockListener.verify();
		mcast.removeAllListeners();
	}

}
