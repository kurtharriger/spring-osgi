/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.extender.internal.blueprint.event;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.springframework.util.ClassUtils;

/**
 * Dispatcher that transforms Spring application context lifecycle events into notifications to the OSGi EventAdmin
 * service.
 * 
 * <b>Note:</b> This class does not assume the EventAdmin service or classes are available. If the classes are missing,
 * the dispatcher will not publish any events during its life time. If the service is unavailable, the dispatcher will
 * stop sending events until the service becomes available.
 * 
 * @author Costin Leau
 */
public class EventAdminDispatcher {

	/** logger */
	private static final Log log;

	/** Whether the Event Admin library is present on the classpath */
	private static final boolean eventAdminAvailable;

	static {
		eventAdminAvailable =
				ClassUtils.isPresent("org.osgi.service.event.EventAdmin", EventAdminDispatcher.class.getClassLoader());

		log = LogFactory.getLog(EventAdminDispatcher.class);

		if (!eventAdminAvailable) {
			log.info("EventAdmin package not found; no Blueprint lifecycle events will be published");
		}
	}

	/**
	 * Actual creation of EventAdmin dispatcher. In separate inner class to avoid runtime dependency on EventAdmin
	 * classes.
	 * 
	 * @author Costin Leau
	 */
	private static abstract class EventAdminDispatcherFactory {

		private static EventDispatcher createDispatcher(BundleContext bundleContext) {
			if (log.isTraceEnabled())
				log.trace("Creating [" + OsgiEventDispatcher.class.getName() + "]");
			return new OsgiEventDispatcher(bundleContext, PublishType.POST);
		}
	}

	/** actual dispatcher */
	private final EventDispatcher dispatcher;

	public EventAdminDispatcher(BundleContext bundleContext) {
		if (eventAdminAvailable) {
			dispatcher = EventAdminDispatcherFactory.createDispatcher(bundleContext);
		} else {
			dispatcher = null;
		}
	}

	public void beforeClose(final BlueprintEvent event) {
		if (dispatcher != null) {
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						public Object run() {
							dispatcher.beforeClose(event);
							return null;
						}
					});
				} else {
					dispatcher.beforeClose(event);
				}
			} catch (Throwable th) {
				log.warn("Cannot dispatch event " + event, th);
			}
		}
	}

	public void beforeRefresh(final BlueprintEvent event) {
		if (dispatcher != null) {
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						public Object run() {
							dispatcher.beforeRefresh(event);
							return null;
						}
					});
				} else {
					dispatcher.beforeRefresh(event);
				}
			} catch (Throwable th) {
				log.warn("Cannot dispatch event " + event, th);
			}
		}
	}

	public void afterClose(final BlueprintEvent event) {
		if (dispatcher != null) {
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						public Object run() {
							dispatcher.afterClose(event);
							return null;
						}
					});
				} else {
					dispatcher.afterClose(event);
				}
			} catch (Throwable th) {
				log.warn("Cannot dispatch event " + event, th);
			}
		}
	}

	public void afterRefresh(final BlueprintEvent event) {
		if (dispatcher != null) {
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						public Object run() {
							dispatcher.afterRefresh(event);
							return null;
						}
					});
				} else {
					dispatcher.afterRefresh(event);
				}
			} catch (Throwable th) {
				log.warn("Cannot dispatch event " + event, th);
			}
		}
	}

	public void refreshFailure(final BlueprintEvent event) {
		if (dispatcher != null) {
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						public Object run() {
							dispatcher.refreshFailure(event);
							return null;
						}
					});
				} else {
					dispatcher.refreshFailure(event);
				}
			} catch (Throwable th) {
				log.warn("Cannot dispatch event " + event, th);
			}
		}
	}

	public void grace(final BlueprintEvent event) {
		if (dispatcher != null) {
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						public Object run() {
							dispatcher.grace(event);
							return null;
						}
					});
				} else {
					dispatcher.grace(event);
				}
			} catch (Throwable th) {
				log.warn("Cannot dispatch event " + event, th);
			}
		}
	}

	public void waiting(final BlueprintEvent event) {
		if (dispatcher != null) {
			try {
				if (System.getSecurityManager() != null) {
					AccessController.doPrivileged(new PrivilegedAction<Object>() {
						public Object run() {
							dispatcher.waiting(event);
							return null;
						}
					});
				} else {
					dispatcher.waiting(event);
				}
			} catch (Throwable th) {
				log.warn("Cannot dispatch event " + event, th);
			}
		}
	}
}