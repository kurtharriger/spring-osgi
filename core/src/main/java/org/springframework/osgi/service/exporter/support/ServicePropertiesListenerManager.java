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

package org.springframework.osgi.service.exporter.support;

/**
 * Service properties change listener manager.
 * 
 * @author Costin Leau
 */
public interface ServicePropertiesListenerManager {

	/**
	 * Adds a listener to be notified of any service properties changes.
	 * 
	 * @param listener service properties change listener
	 */
	void addListener(ServicePropertiesChangeListener listener);

	/**
	 * Removes a listener interested in service properties changes.
	 * 
	 * @param listener service properties change listener
	 */
	void removeListener(ServicePropertiesChangeListener listener);
}
