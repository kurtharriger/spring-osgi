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

package org.springframework.osgi.config.internal.adapter;

import org.springframework.beans.factory.FactoryBean;

/**
 * Simple adapter class used for maintaing configuration compatibility when
 * using <interface> parameter with classes instead of class names.
 * 
 * @author Costin Leau
 */
public class ToStringClassAdapter implements FactoryBean<String> {

	private final String toString;


	private ToStringClassAdapter(Object target) {
		if (target instanceof Class) {
			toString = ((Class<?>) target).getName();
		}
		else {
			toString = (target == null ? "" : target.toString());
		}
	}

	public String getObject() throws Exception {
		return toString;
	}

	public Class<? extends String> getObjectType() {
		return String.class;
	}

	public boolean isSingleton() {
		return true;
	}
}
