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
package org.springframework.osgi.blueprint.reflect;

import java.util.Collections;
import java.util.List;

import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * Dedicated metadata class for environment managers.
 * 
 * @author Costin Leau
 */
class EnvironmentManagerMetadata implements ComponentMetadata {

	private final String id;

	public EnvironmentManagerMetadata(String id) {
		this.id = id;
	}

	public int getActivation() {
		return ComponentMetadata.ACTIVATION_EAGER;
	}

	public List<String> getDependsOn() {
		return Collections.emptyList();
	}

	public String getId() {
		return id;
	}
}
