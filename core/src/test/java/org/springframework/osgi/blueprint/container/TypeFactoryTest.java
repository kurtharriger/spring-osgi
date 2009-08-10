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
package org.springframework.osgi.blueprint.container;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.osgi.service.blueprint.container.ReifiedType;
import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Costin Leau
 */
public class TypeFactoryTest extends TestCase {

	public void testJdk4Classes() throws Exception {
		TypeDescriptor desc = TypeDescriptor.forObject(new ArrayList());
		ReifiedType tp = TypeFactory.getType(desc);
		assertEquals(0, tp.size());
		assertEquals(ArrayList.class, tp.getRawClass());
	}
}
