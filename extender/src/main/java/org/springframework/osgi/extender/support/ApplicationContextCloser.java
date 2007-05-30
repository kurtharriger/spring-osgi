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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.osgi.context.support.SpringBundleEvent;

/**
 * Closes an application context created by the extender bundle.
 * The context may be fully initialized, or may still be in the process
 * of initializing (for example, waiting on service dependencies).
 *
 * @author Adrian Colyer
 */
public class ApplicationContextCloser {

	private static final Log log = LogFactory.getLog(ApplicationContextCloser.class);

	private final Bundle bundle;
	private final Map applicationContextMap;
	private final Map contextsPendingInitializationMap;
	private final ApplicationEventMulticaster mcast;
	private final Map pendingRegistrationTasksMap;

	public ApplicationContextCloser(Bundle bundle, Map contextMap,
	                                Map initMap, Map pendingRegistrationTasks, ApplicationEventMulticaster mcast) {
		this.bundle = bundle;
		this.applicationContextMap = contextMap;
		this.contextsPendingInitializationMap = initMap;
		this.pendingRegistrationTasksMap = pendingRegistrationTasks;
		this.mcast = mcast;
	}

	public void close() {
        ApplicationContextCreator creator = null;
        ServiceDependentOsgiApplicationContext appContext;
		Long bundleKey = new Long(this.bundle.getBundleId());

		// Check for tasks that have not closed yet
		synchronized(pendingRegistrationTasksMap) {
			if (pendingRegistrationTasksMap.remove(bundleKey) != null) {
                return;
			}
		}
		postEvent(BundleEvent.STOPPING);
		// do not change locking order without also changing ApplicationContextCreator
		synchronized (this.applicationContextMap) {
			synchronized (this.contextsPendingInitializationMap) {
				appContext = (ServiceDependentOsgiApplicationContext) this.applicationContextMap.remove(bundleKey);
				if (appContext == null) {
					// no fully initialised app context for this bundle,
					// is there one pending initialisation?
					creator = (ApplicationContextCreator) this.contextsPendingInitializationMap.remove(bundleKey);
				}
			}
		}

        if (creator != null) {
			if (log.isInfoEnabled()) {
				log.info("Closing application context which is in creation process for bundle [" +
					bundle.getSymbolicName() + "]");
			}
            appContext = creator.getContext();
            appContext.interrupt();
            Thread creationThread = creator.getCreationThread();
            if (creationThread != null) {
                creationThread.interrupt();
            }
        }

        if (appContext != null && appContext.getState() == ContextState.CREATED) {
			if (log.isInfoEnabled()) {
				log.info("Closing application context for bundle [" +
					bundle.getSymbolicName() + "]");
			}
			appContext.close();
		} else {
			if (log.isInfoEnabled()) {
				log.info("Application context creation has been interrupted for bundle [" +
					bundle.getSymbolicName() + "]");
			}
        }
		postEvent(BundleEvent.STOPPED);
	}

	private void postEvent(int starting) {
		mcast.multicastEvent(new SpringBundleEvent(starting, bundle));
	}
}
