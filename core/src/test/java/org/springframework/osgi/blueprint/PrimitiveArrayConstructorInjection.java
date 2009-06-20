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
package org.springframework.osgi.blueprint;

/**
 * @author Costin Leau
 */
public class PrimitiveArrayConstructorInjection {

	private Object obj;

	public PrimitiveArrayConstructorInjection(int[] array) {
		this.obj = array;
	}

	public PrimitiveArrayConstructorInjection(float[] array) {
		this.obj = array;
	}

	public PrimitiveArrayConstructorInjection(short[] array) {
		this.obj = array;
	}

	public PrimitiveArrayConstructorInjection(byte[] array) {
		this.obj = array;
	}

	public PrimitiveArrayConstructorInjection(double[] array) {
		this.obj = array;
	}

	public PrimitiveArrayConstructorInjection(boolean[] array) {
		this.obj = array;
	}

	public Object getObj() {
		return obj;
	}
}
