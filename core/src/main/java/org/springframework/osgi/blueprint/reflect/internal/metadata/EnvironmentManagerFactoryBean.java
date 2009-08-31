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
package org.springframework.osgi.blueprint.reflect.internal.metadata;

import org.springframework.beans.factory.FactoryBean;

/**
 * Basic FactoryBean acting as a wrapper around environment beans. Since usually these are already instantiated, to
 * allow registration of bean definitions inside the container, this 'special' class is used so it can be identified
 * when creating the blueprint environment metadata.
 * 
 * @author Costin Leau
 */
public class EnvironmentManagerFactoryBean implements FactoryBean<Object> {

	private final Object instance;

	public EnvironmentManagerFactoryBean(Object instance) {
		this.instance = instance;
	}

	public Object getObject() throws Exception {
		return instance;
	}

	public Class<?> getObjectType() {
		return instance.getClass();
	}

	public boolean isSingleton() {
		return true;
	}
}
