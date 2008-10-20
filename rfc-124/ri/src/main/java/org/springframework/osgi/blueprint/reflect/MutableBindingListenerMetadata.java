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

import org.osgi.service.blueprint.reflect.BindingListenerMetadata;
import org.osgi.service.blueprint.reflect.Value;

public class MutableBindingListenerMetadata implements BindingListenerMetadata {

	private String bindMethod;
	private String unbindMethod;
	private final Value listenerComponent;
	
	public MutableBindingListenerMetadata(Value v, String bind, String unbind) {
		if (null == v) {
			throw new IllegalArgumentException("referenced listener cannot be null");
		}
		this.listenerComponent = v;
		this.bindMethod = bind;
		this.unbindMethod = unbind;
	}
	
	public String getBindMethodName() {
		return this.bindMethod;
	}
	
	public void setBindMethod(String bindMethod) {
		this.bindMethod = bindMethod;
	}

	public String getUnbindMethodName() {
		return this.unbindMethod;
	}
	
	public void setUnbindMethod(String unbindMethod) {
		this.unbindMethod = unbindMethod;
	}

	public Value getListenerComponent() {
		return this.listenerComponent;
	}

}
