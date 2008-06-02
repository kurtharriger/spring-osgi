/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.service.importer.support.internal.controller;

import org.springframework.osgi.service.util.internal.controller.ControllerRegistry;
import org.springframework.osgi.service.util.internal.controller.GenericRegistry;

/**
 * Importer-only delegate (it would be nice to have generics).
 * 
 * @author Costin Leau
 * 
 */
public abstract class ImporterRegistry {

	/** controller delegate */
	private static final ControllerRegistry delegate = new GenericRegistry();


	public static void putController(Object slave, ImporterInternalActions master) {
		delegate.putController(slave, master);
	}

	public static ImporterInternalActions getControllerFor(Object slave) {
		return (ImporterInternalActions) delegate.getControllerFor(slave);
	}

	public static void removeController(Object slave) {
		delegate.removeController(slave);
	}
}
