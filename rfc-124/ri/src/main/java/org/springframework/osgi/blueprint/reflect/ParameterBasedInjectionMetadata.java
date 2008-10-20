/*
 * Copyright 2008 the original author or authors.
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

import org.osgi.service.blueprint.reflect.ParameterSpecification;

public abstract class ParameterBasedInjectionMetadata {

	private ParameterSpecification[] params = new ParameterSpecification[0];

	public ParameterBasedInjectionMetadata(ParameterSpecification[] paramSpecs) {
		if (paramSpecs != null) {
			for (ParameterSpecification ps : paramSpecs) {
				if (null == ps) {
					throw new IllegalArgumentException("parameter specification cannot be null");
				}
			}
			this.params = paramSpecs;
		}
	}

	public ParameterSpecification[] getParameterSpecifications() {
		return this.params;
	}

	public void setParameterSpecifiations(ParameterSpecification[] paramSpecs) {
		if (null == paramSpecs) {
			this.params = new ParameterSpecification[0];
		}
		else {
			for (ParameterSpecification ps : paramSpecs) {
				if (null == ps) {
					throw new IllegalArgumentException("parameter specification cannot be null");
				}
			}			
			this.params = paramSpecs;
		}
	}

}