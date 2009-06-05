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

package org.springframework.osgi.extender.internal.support;

import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticasterAdapter;
import org.springframework.osgi.extender.internal.dependencies.startup.MandatoryImporterDependencyFactory;
import org.springframework.osgi.mock.ArrayEnumerator;
import org.springframework.osgi.mock.MockBundle;
import org.springframework.osgi.mock.MockBundleContext;

/**
 * @author Costin Leau
 */
public class ExtenderConfigurationCustomSettingsTest extends TestCase {

	private ExtenderConfiguration config;
	private BundleContext bundleContext;
	private Bundle bundle;

	protected void setUp() throws Exception {
		bundle = new MockBundle() {

			public Enumeration findEntries(String path, String filePattern, boolean recurse) {
				return new ArrayEnumerator(new URL[] { getClass().getResource(
						"/org/springframework/osgi/extender/internal/support/extender-custom-config.xml") });
			}
		};

		bundleContext = new MockBundleContext(bundle);
		config = new ExtenderConfiguration(bundleContext, LogFactory.getLog(ExtenderConfiguration.class));
	}

	protected void tearDown() throws Exception {
		config.destroy();
		config = null;
	}

	public void testTaskExecutor() throws Exception {
		assertTrue(config.getTaskExecutor() instanceof SimpleAsyncTaskExecutor);
		assertEquals("conf-extender-thread", ((SimpleAsyncTaskExecutor) config.getTaskExecutor()).getThreadNamePrefix());
	}

	public void testShutdownTaskExecutor() throws Exception {
		TaskExecutor executor = config.getShutdownTaskExecutor();
		assertTrue(executor instanceof SimpleAsyncTaskExecutor);
	}

	public void testEventMulticaster() throws Exception {
		assertTrue(config.getEventMulticaster() instanceof OsgiBundleApplicationContextEventMulticasterAdapter);
	}

	public void testApplicationContextCreator() throws Exception {
		assertTrue(config.getContextCreator() instanceof DummyContextCreator);
	}

	public void testShutdownWaitTime() throws Exception {
		// 300 ms
		assertEquals(300, config.getShutdownWaitTime());
	}

	public void testShouldProcessAnnotation() throws Exception {
		assertTrue(config.shouldProcessAnnotation());
	}

	public void testDependencyWaitTime() throws Exception {
		// 200 ms
		assertEquals(200, config.getDependencyWaitTime());
	}

	public void testPostProcessors() throws Exception {
		List postProcessors = config.getPostProcessors();
		assertEquals(1, postProcessors.size());
		assertTrue(postProcessors.get(0) instanceof DummyProcessor);
	}

	public void testDependencyFactories() throws Exception {
		List factories = config.getDependencyFactories();
		assertEquals("wrong number of dependencies factories registered by default", 1, factories.size());
		assertTrue(factories.get(0) instanceof MandatoryImporterDependencyFactory);
	}
}