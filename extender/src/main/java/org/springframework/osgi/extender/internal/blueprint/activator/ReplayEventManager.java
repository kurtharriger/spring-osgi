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

	private final Map<Bundle, BlueprintEvent> events =
			Collections.synchronizedMap(new LinkedHashMap<Bundle, BlueprintEvent>());

	private final BundleContext bundleContext;
	private final BundleListener listener = new BundleListener() {

		public void bundleChanged(BundleEvent event) {
			if (BundleEvent.STOPPED == event.getType()) {
				events.remove(event.getBundle());
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
		events.put(replay.getBundle(), replay);
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
