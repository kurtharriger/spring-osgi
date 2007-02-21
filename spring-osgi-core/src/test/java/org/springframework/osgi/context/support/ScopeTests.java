/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.context.support;

import java.util.Properties;

import junit.framework.TestCase;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

public class ScopeTests extends TestCase {

	private static Object tag;

	private abstract class AbstractScope implements Scope {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.beans.factory.config.Scope#getConversationId()
		 */
		public String getConversationId() {
			return null;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.beans.factory.config.Scope#registerDestructionCallback(java.lang.String,
		 * java.lang.Runnable)
		 */
		public void registerDestructionCallback(String name, Runnable callback) {
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.beans.factory.config.Scope#remove(java.lang.String)
		 */
		public Object remove(String name) {
			return null;
		}
	}

	private class FooScope extends AbstractScope {

		private Object cachedObject = new Object();

		public Object get(String name, ObjectFactory objectFactory) {
			System.out.println("tag is " + tag);
			System.out.println("requested " + name + " w/ objFact " + objectFactory);
			// if (name.equals("a")) {
			// System.out.println("returning cached copy " + cachedObject);
			// return cachedObject;
			// }

			Object obj = objectFactory.getObject();
			System.out.println("obj is " + obj);
			if (tag != null)
				tag = "setting tag to object " + obj;
			return obj;
		}

	}

	private ConfigurableBeanFactory bf;

	protected void setUp() throws Exception {
		Resource file = new ClassPathResource("scopes.xml");
		bf = new XmlBeanFactory(file);
		bf.registerScope("foo", new FooScope());
		bf.registerScope("bar", new FooScope());
	}

	protected void tearDown() throws Exception {
		bf.destroySingletons();
	}

	public void testScopes() throws Exception {
		tag = new String("set tag before a");
		Object a = bf.getBean("a");
		System.out.println("tag after a " + tag);
		System.out.println("request a;got=" + a);
		System.out.println("a class is" + a.getClass());
		((Properties) a).put("goo", "foo");

		Object b = bf.getBean("b");
		System.out.println("request b;got=" + b);
		System.out.println("b class is" + b.getClass());
		b = bf.getBean("b");
		System.out.println("request b;got=" + b);
		System.out.println("b class is" + b.getClass());

		Object scopedA = bf.getBean("a");
		System.out.println(scopedA.getClass());
		System.out.println(ObjectUtils.nullSafeToString(ClassUtils.getAllInterfaces(scopedA)));
	}
}
