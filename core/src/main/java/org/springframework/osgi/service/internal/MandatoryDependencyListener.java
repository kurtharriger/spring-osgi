/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.service.internal;

/**
 * Internal framework listener notified when a mandatory service importer comes
 * up or down.
 * 
 * @author Costin Leau
 * 
 */
public interface MandatoryDependencyListener {

	/**
	 * Called when at the mandatory dependency for an importer has been
	 * satisfied. This method is called only if the dependency has been
	 * unsatisfied before.
	 * 
	 * @see #mandatoryServiceUnsatisfied()
	 */
	void mandatoryServiceSatisfied(MandatoryDependencyEvent event);

	/**
	 * Called when no service for a mandatory service was found.
	 */
	void mandatoryServiceUnsatisfied(MandatoryDependencyEvent event);
}
