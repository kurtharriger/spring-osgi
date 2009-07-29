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

import java.lang.reflect.Constructor;

import junit.framework.TestCase;

public class ReflectionTest extends TestCase {

	static class Foo {
		public Foo(boolean bool) {
			System.out.println("boolean " + bool);
		}

		public Foo(Boolean bool) {
			System.out.println("Boolean " + bool);
		}
	};

	public void testPrimitive() throws Exception {
		Constructor[] constructors = Foo.class.getDeclaredConstructors();
		for (Constructor constructor : constructors) {
			Class[] parameterTypes = constructor.getParameterTypes();
			for (Class class1 : parameterTypes) {
				System.out.println(class1.getName());
			}
		}

		boolean obj = true;
		
		Foo foo = new Foo(Boolean.TRUE);
		foo = new Foo(obj);
		foo = new Foo((Boolean) true);
	}
}
