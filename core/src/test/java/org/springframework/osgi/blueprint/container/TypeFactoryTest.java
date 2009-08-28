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

import java.awt.Point;
import java.awt.Shape;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.osgi.service.blueprint.container.ReifiedType;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Costin Leau
 */
public class TypeFactoryTest extends TestCase {

	private static class TestSet<A> {

		public void rawList(List arg) {
		}

		public void primitive(int arg) {
		}

		public void integer(Integer arg) {
		}

		public void typedList(LinkedList<Point> arg) {
		};

		public void extendsList(LinkedList<? extends Shape> arg) {
		};

		public void superList(LinkedList<? super Shape> arg) {
		};

		public void typedMap(TreeMap<Integer, Double> arg) {
		};

		public void pointMap(TreeMap<String, Point> arg) {
		}

		public void typedReference(AtomicReference<Boolean> arg) {
		}

		public void objectTypedReference(AtomicReference<Object> arg) {
		}

		public void wildcardReference(AtomicReference<?> arg) {
		}

		public void superTypedReference(AtomicReference<? super Properties> arg) {
		}

		public void extendsTypedReference(AtomicReference<? extends Properties> arg) {
		}

		public void typeVariable(AtomicReference<A> arg) {
		}
	}

	public void testJdk4Classes() throws Exception {
		ReifiedType tp = getReifiedTypeFor("rawList");
		assertEquals(1, tp.size());
		assertEquals(List.class, tp.getRawClass());
	}

	public void testPrimitive() throws Exception {
		ReifiedType tp = getReifiedTypeFor("primitive");
		assertEquals(0, tp.size());
		assertEquals(Integer.class, tp.getRawClass());
	}

	public void testInteger() throws Exception {
		ReifiedType tp = getReifiedTypeFor("integer");
		assertEquals(0, tp.size());
		assertEquals(Integer.class, tp.getRawClass());
	}

	public void testTypedObjectList() throws Exception {
		ReifiedType tp = getReifiedTypeFor("typedList");
		assertEquals(1, tp.size());
		assertEquals(LinkedList.class, tp.getRawClass());
		assertEquals(Point.class, tp.getActualTypeArgument(0).getRawClass());
	}

	public void testExtendsList() throws Exception {
		ReifiedType tp = getReifiedTypeFor("extendsList");
		assertEquals(1, tp.size());
		assertEquals(LinkedList.class, tp.getRawClass());
		assertEquals(Shape.class, tp.getActualTypeArgument(0).getRawClass());
	}

	public void testSuperList() throws Exception {
		ReifiedType tp = getReifiedTypeFor("superList");
		assertEquals(1, tp.size());
		assertEquals(LinkedList.class, tp.getRawClass());
		assertEquals(Shape.class, tp.getActualTypeArgument(0).getRawClass());
	}

	public void testTypedMap() throws Exception {
		ReifiedType tp = getReifiedTypeFor("typedMap");
		assertEquals(2, tp.size());
		assertEquals(TreeMap.class, tp.getRawClass());
		assertEquals(Integer.class, tp.getActualTypeArgument(0).getRawClass());
		assertEquals(Double.class, tp.getActualTypeArgument(1).getRawClass());
	}

	public void testPointMap() throws Exception {
		ReifiedType tp = getReifiedTypeFor("pointMap");
		assertEquals(2, tp.size());
		assertEquals(TreeMap.class, tp.getRawClass());
		assertEquals(String.class, tp.getActualTypeArgument(0).getRawClass());
		assertEquals(Point.class, tp.getActualTypeArgument(1).getRawClass());
	}

	public void testTypedReference() throws Exception {
		ReifiedType tp = getReifiedTypeFor("typedReference");
		assertEquals(1, tp.size());
		assertEquals(AtomicReference.class, tp.getRawClass());
		assertEquals(Boolean.class, tp.getActualTypeArgument(0).getRawClass());
	}

	public void testObjectTypedReference() throws Exception {
		ReifiedType tp = getReifiedTypeFor("objectTypedReference");
		assertEquals(1, tp.size());
		assertEquals(AtomicReference.class, tp.getRawClass());
		assertEquals(Object.class, tp.getActualTypeArgument(0).getRawClass());
	}

	public void testWildcardReference() throws Exception {
		ReifiedType tp = getReifiedTypeFor("wildcardReference");
		assertEquals(1, tp.size());
		assertEquals(AtomicReference.class, tp.getRawClass());
		assertEquals(Object.class, tp.getActualTypeArgument(0).getRawClass());
	}

	public void testSuperReference() throws Exception {
		ReifiedType tp = getReifiedTypeFor("superTypedReference");
		assertEquals(1, tp.size());
		assertEquals(AtomicReference.class, tp.getRawClass());
		assertEquals(Properties.class, tp.getActualTypeArgument(0).getRawClass());
	}

	public void testExtendsReference() throws Exception {
		ReifiedType tp = getReifiedTypeFor("extendsTypedReference");
		assertEquals(1, tp.size());
		assertEquals(AtomicReference.class, tp.getRawClass());
		assertEquals(Properties.class, tp.getActualTypeArgument(0).getRawClass());
	}

	public void testTypeVariable() throws Exception {
		ReifiedType tp = getReifiedTypeFor("typeVariable");
		assertEquals(1, tp.size());
		assertEquals(AtomicReference.class, tp.getRawClass());
		assertEquals(Object.class, tp.getActualTypeArgument(0).getRawClass());
	}

	private ReifiedType getReifiedTypeFor(String methodName) {
		Method mt = BeanUtils.findDeclaredMethodWithMinimalParameters(TestSet.class, methodName);
		TypeDescriptor td = new TypeDescriptor(new MethodParameter(mt, 0));
		return TypeFactory.getType(td);
	}
}