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
package org.springframework.osgi.compendium.internal.cm.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.springframework.osgi.service.exporter.support.ServicePropertiesChangeEvent;
import org.springframework.osgi.service.exporter.support.ServicePropertiesChangeListener;
import org.springframework.osgi.service.exporter.support.ServicePropertiesListenerManager;

/**
 * Basic implementation of {@link ServicePropertiesChangeListener}.
 * 
 * @author Costin Leau
 */
public class ChangeableProperties extends Properties implements ServicePropertiesListenerManager {

	private List<ServicePropertiesChangeListener> listeners =
			Collections.synchronizedList(new ArrayList<ServicePropertiesChangeListener>(4));

	public void addListener(ServicePropertiesChangeListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	public void removeListener(ServicePropertiesChangeListener listener) {
		if (listener != null) {
			listeners.remove(listener);
		}
	}

	public void notifyListeners() {
		ServicePropertiesChangeEvent event = new ServicePropertiesChangeEvent(this);
		synchronized (listeners) {
			for (ServicePropertiesChangeListener listener : listeners) {
				listener.propertiesChange(event);
			}
		}
	}
}
