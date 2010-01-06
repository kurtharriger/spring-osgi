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
package org.springframework.osgi.compendium.internal.cm;

import java.util.Map;

import org.springframework.util.ObjectUtils;

/**
 * A chain up managed updates.
 * 
 * @author Costin Leau
 */
class ChainedManagedUpdate implements UpdateCallback {

	private final UpdateCallback[] callbacks;

	ChainedManagedUpdate(UpdateCallback[] callbacks) {
		this.callbacks = (ObjectUtils.isEmpty(callbacks) ? new UpdateCallback[0] : callbacks);
	}

	public void update(Object instance, Map properties) {
		for (UpdateCallback callback : callbacks) {
			callback.update(instance, properties);
		}
	}
}
