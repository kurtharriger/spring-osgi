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
package org.springframework.osgi.extender.internal.activator;

import junit.framework.TestCase;

import org.springframework.core.GenericTypeResolver;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleContextClosedEvent;
import org.springframework.osgi.extender.internal.activator.GenericsTest.SomeClass.AnotherClass.NestedListener;

/**
 * Basic generic detection test.
 * 
 * @author Costin Leau
 */
public class GenericsTest extends TestCase {

	public static class SomeClass {
		public static class AnotherClass {
			public static class NestedListener implements
					OsgiBundleApplicationContextListener<OsgiBundleContextClosedEvent> {

				public void onOsgiApplicationEvent(OsgiBundleContextClosedEvent event) {
				}
			}
		}
	}

	public void testRawType() throws Exception {
		assertSame(null, GenericTypeResolver.resolveTypeArgument(RawListener.class,
				OsgiBundleApplicationContextListener.class));
	}

	public void testGenericType() throws Exception {
		assertSame(OsgiBundleApplicationContextEvent.class, GenericTypeResolver.resolveTypeArgument(
				GenericListener.class, OsgiBundleApplicationContextListener.class));
	}

	public void testSpecializedType() throws Exception {
		assertSame(OsgiBundleContextClosedEvent.class, GenericTypeResolver.resolveTypeArgument(
				SpecializedListener.class, OsgiBundleApplicationContextListener.class));
	}

	public void testNestedListener() throws Exception {
		assertSame(OsgiBundleContextClosedEvent.class, GenericTypeResolver.resolveTypeArgument(NestedListener.class,
				OsgiBundleApplicationContextListener.class));
	}
}
