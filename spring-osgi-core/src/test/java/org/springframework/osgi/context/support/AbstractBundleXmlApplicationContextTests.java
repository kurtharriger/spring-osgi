/*
 * Copyright 2002-2006 the original author or authors.
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

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * 
 * @author Costin Leau
 */
public class AbstractBundleXmlApplicationContextTests extends TestCase {

	AbstractBundleXmlApplicationContext xmlContext;

	MockControl bundleCtxCtrl, bundleCtrl;
	BundleContext context;
	Bundle bundle;

	Dictionary dictionary;

	protected void setUp() throws Exception {
		bundleCtxCtrl = MockControl.createNiceControl(BundleContext.class);
		context = (BundleContext) bundleCtxCtrl.getMock();
		bundleCtrl = MockControl.createNiceControl(Bundle.class);
		bundle = (Bundle) bundleCtrl.getMock();

		bundleCtxCtrl.expectAndReturn(context.getBundle(), bundle, MockControl.ONE_OR_MORE);

		dictionary = new Hashtable();
		dictionary.put(AbstractBundleXmlApplicationContext.APPLICATION_CONTEXT_SERVICE_NAME_HEADER, "Shadow Play");

		// allow headers to be taken multiple times
		bundleCtrl.expectAndReturn(bundle.getHeaders(), dictionary, MockControl.ONE_OR_MORE);

	}

	private void createContext() {
		xmlContext = new AbstractBundleXmlApplicationContext(context, new String[] {}) {
		};
	}

	protected void tearDown() throws Exception {
		// bundleCtxCtrl.verify();
		// bundleCtrl.verify();
		context = null;
		bundleCtxCtrl = null;
		xmlContext = null;
		bundle = null;
		bundleCtrl = null;
	}

	public void testGetBundleName() {
		String symbolicName = "symbolic";
		bundleCtrl.reset();
		bundleCtrl.expectAndReturn(bundle.getSymbolicName(), symbolicName, MockControl.ONE_OR_MORE);
		bundleCtxCtrl.replay();
		bundleCtrl.replay();

		// check default
		createContext();

		assertEquals(symbolicName, xmlContext.getBundleName());
	}

	public void testGetBundleNameFallbackMechanism() {
		bundleCtxCtrl.replay();
		bundleCtrl.replay();

		String title = "Phat City";
		dictionary.put(Constants.BUNDLE_NAME, title);

		// check default
		createContext();

		// use the 2 symbolic name calls
		assertEquals(title, xmlContext.getBundleName());
	}

	public void testGetServiceName() {
		bundleCtxCtrl.replay();
		bundleCtrl.replay();

		// use defaults
		String title = "Shadow Play";
		dictionary.put(AbstractBundleXmlApplicationContext.APPLICATION_CONTEXT_SERVICE_NAME_HEADER, title);

		createContext();
		assertEquals(title, xmlContext.getServiceName());

	}

	public void testGetServiceNameFallbackMechanism() {
		bundleCtxCtrl.replay();
		bundleCtrl.replay();

		String title = "Enchantment";
		dictionary.put(Constants.BUNDLE_NAME, title);
		dictionary.remove(AbstractBundleXmlApplicationContext.APPLICATION_CONTEXT_SERVICE_NAME_HEADER);

		createContext();
		assertEquals(title + "-springApplicationContext", xmlContext.getServiceName());
	}

	public void testGetParentApplicationContext() throws Exception {
		String filter = "(org-springframework-context-service-name=goo)";
		ServiceReference servRef = (ServiceReference) MockControl.createControl(ServiceReference.class).getMock();

		ApplicationContext parentCtx = new GenericApplicationContext();
		bundleCtxCtrl.expectAndReturn(context.getServiceReferences(ApplicationContext.class.getName(), filter),
				new ServiceReference[] { servRef });
		bundleCtxCtrl.expectAndReturn(context.getService(servRef), parentCtx);

		bundleCtxCtrl.replay();
		bundleCtrl.replay();

		createContext();
		assertNull(xmlContext.getParentApplicationContext(context));

		dictionary.put(AbstractBundleXmlApplicationContext.PARENT_CONTEXT_SERVICE_NAME_HEADER, "goo");
		assertSame(parentCtx, xmlContext.getParentApplicationContext(context));
	}
}
