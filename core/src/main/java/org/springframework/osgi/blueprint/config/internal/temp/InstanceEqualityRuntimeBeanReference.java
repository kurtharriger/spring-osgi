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
package org.springframework.osgi.blueprint.config.internal.temp;

import org.springframework.beans.factory.config.RuntimeBeanReference;

/**
 * A temporary runtime bean reference that does not implement the equality contract to prevent set merges.
 * 
 * @author Costin Leau
 */
//FIXME: delete when SPR-5861 is fixed
public class InstanceEqualityRuntimeBeanReference extends RuntimeBeanReference {

	public InstanceEqualityRuntimeBeanReference(String beanName, boolean toParent) {
		super(beanName, toParent);
	}

	public InstanceEqualityRuntimeBeanReference(String beanName) {
		super(beanName);
	}

	@Override
	public boolean equals(Object other) {
		return this == other;
	}
}
