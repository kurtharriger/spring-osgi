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
package org.springframework.osgi.extender.internal.blueprint.activator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.service.blueprint.container.BlueprintEvent;
import org.osgi.service.blueprint.container.BlueprintListener;

/**
 * Class managing blueprint replay events.
 * 
 * @author Costin Leau
 */
class ReplayEventManager {

	/** logger */
	private static final Log log = LogFactory.getLog(ReplayEventManager.class);

	private final Map<Bundle, BlueprintEvent> events =
			Collections.synchronizedMap(new LinkedHashMap<Bundle, BlueprintEvent>());

	private final BundleContext bundleContext;
	private final BundleListener listener = new BundleListener() {

		public void bundleChanged(BundleEvent event) {
			if (BundleEvent.STOPPED == event.getType() || BundleEvent.UNINSTALLED == event.getType()
					|| BundleEvent.UNRESOLVED == event.getType()) {
				BlueprintEvent removed = events.remove(event.getBundle());
				if (log.isTraceEnabled())
					log.trace("Removed  bundle " + event.getBundle() + " for sending replayes events; last one was "
							+ removed);
			}
		}
	};

	ReplayEventManager(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		bundleContext.addBundleListener(listener);
	}

	void addEvent(BlueprintEvent event) {
		// copy event
		BlueprintEvent replay = new BlueprintEvent(event, true);
		Bundle bnd = replay.getBundle();
		if (bnd.getState() == Bundle.ACTIVE || bnd.getState() == Bundle.STARTING || bnd.getState() == Bundle.STOPPING) {
			events.put(bnd, replay);
			if (log.isTraceEnabled())
				log.trace("Adding replay event  " + replay.getType() + " for bundle " + replay.getBundle());
		} else {
			if (log.isTraceEnabled()) {
				log.trace("Replay event " + replay.getType() + " ignored; " + "owning bundle has been uninstalled "
						+ bnd);
				events.remove(bnd);
			}
		}
	}

	void destroy() {
		events.clear();
		try {
			bundleContext.removeBundleListener(listener);
		} catch (Exception ex) {
			// discard
		}
	}

	void dispatchReplayEvents(BlueprintListener listener) {
		synchronized (events) {
			for (BlueprintEvent event : events.values()) {
				listener.blueprintEvent(event);
			}
		}
	}
}