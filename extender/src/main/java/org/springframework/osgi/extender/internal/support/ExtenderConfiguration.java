/*
 * Copyright 2006-2008 the original author or authors.
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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticaster;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticasterAdapter;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.extender.OsgiApplicationContextCreator;
import org.springframework.osgi.extender.OsgiBeanFactoryPostProcessor;
import org.springframework.osgi.extender.support.DefaultOsgiApplicationContextCreator;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Configuration class for the extender. Takes care of locating the extender
 * specific configurations and merging the results with the defaults.
 * 
 * <p/> Note that this configuration will consider mandatory options required by
 * 
 * @author Costin Leau
 * 
 */
public class ExtenderConfiguration implements DisposableBean {

	/** logger */
	private static final Log log = LogFactory.getLog(ExtenderConfiguration.class);

	private static final String TASK_EXECUTOR_NAME = "taskExecutor";

	private static final String CONTEXT_CREATOR_NAME = "applicationContextCreator";

	private static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "osgiApplicationEventMulticaster";

	private static final String PROPERTIES_NAME = "extenderProperties";

	private static final String SHUTDOWN_WAIT_KEY = "shutdown.wait.time";

	private static final String PROCESS_ANNOTATIONS_KEY = "process.annotations";

	private static final String EXTENDER_CFG_LOCATION = "META-INF/spring/extender";

	/**
	 * old configuration location
	 * 
	 * @deprecated
	 */
	private static final String OLD_EXTENDER_CFG_LOCATION = "META-INF/spring";

	private static final String XML_PATTERN = "*.xml";

	//
	// defaults
	//
	private static final long DEFAULT_SHUTDOWN_WAIT = 10 * 1000;
	private static final boolean DEFAULT_PROCESS_ANNOTATION = false;

	private ConfigurableOsgiBundleApplicationContext extenderConfiguration;

	private TaskExecutor taskExecutor;

	private boolean isTaskExecutorManagedInternally = false;

	private boolean isMulticasterManagedInternally = false;

	private long shutdownWaitTime;

	private boolean processAnnotation;

	private OsgiBundleApplicationContextEventMulticaster eventMulticaster;

	private boolean forceThreadShutdown;

	private OsgiApplicationContextCreator contextCreator;

	/** bundle wrapped class loader */
	private ClassLoader classLoader;
	/** List of context post processors */
	private List postProcessors = new ArrayList(0);


	/**
	 * Constructs a new <code>ExtenderConfiguration</code> instance. Locates
	 * the extender configuration, creates an application context which will
	 * returned the extender items.
	 * 
	 * @param bundleContext extender OSGi bundle context
	 */
	public ExtenderConfiguration(BundleContext bundleContext) {
		Bundle bundle = bundleContext.getBundle();
		Properties properties = new Properties(createDefaultProperties());

		Enumeration enm = bundle.findEntries(EXTENDER_CFG_LOCATION, XML_PATTERN, false);

		Enumeration oldConfiguration = bundle.findEntries(OLD_EXTENDER_CFG_LOCATION, XML_PATTERN, false);

		if (enm == null && oldConfiguration == null) {
			log.info("No custom configuration detected; using defaults");

			taskExecutor = createDefaultTaskExecutor();
			eventMulticaster = createDefaultEventMulticaster();

			isMulticasterManagedInternally = true;
			contextCreator = createDefaultApplicationContextCreator();
			classLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle);
		}
		else {
			String[] newConfigs = copyEnumerationToList(enm);
			String[] oldConfigs = copyEnumerationToList(oldConfiguration);

			if (!ObjectUtils.isEmpty(oldConfigs)) {
				log.warn("Extender configuration location [" + OLD_EXTENDER_CFG_LOCATION
						+ "] has been deprecated and will be removed after RC1; use [" + EXTENDER_CFG_LOCATION
						+ "] instead");
			}

			// merge old configs first so the new file can override bean definitions (if needed)
			String[] configs = StringUtils.mergeStringArrays(oldConfigs, newConfigs);

			log.info("Detected custom configurations " + ObjectUtils.nullSafeToString(configs));
			// create OSGi specific XML context
			extenderConfiguration = new OsgiBundleXmlApplicationContext(configs);
			extenderConfiguration.setBundleContext(bundleContext);
			extenderConfiguration.refresh();

			// initialize beans

			taskExecutor = extenderConfiguration.containsBean(TASK_EXECUTOR_NAME) ? (TaskExecutor) extenderConfiguration.getBean(
				TASK_EXECUTOR_NAME, TaskExecutor.class)
					: createDefaultTaskExecutor();

			eventMulticaster = extenderConfiguration.containsBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) ? (OsgiBundleApplicationContextEventMulticaster) extenderConfiguration.getBean(
				APPLICATION_EVENT_MULTICASTER_BEAN_NAME, OsgiBundleApplicationContextEventMulticaster.class)
					: createDefaultEventMulticaster();

			contextCreator = extenderConfiguration.containsBean(CONTEXT_CREATOR_NAME) ? (OsgiApplicationContextCreator) extenderConfiguration.getBean(
				CONTEXT_CREATOR_NAME, OsgiApplicationContextCreator.class)
					: createDefaultApplicationContextCreator();

			// get post processors
			postProcessors.addAll(extenderConfiguration.getBeansOfType(OsgiBeanFactoryPostProcessor.class).values());

			classLoader = extenderConfiguration.getClassLoader();
			// extender properties using the defaults as backup
			if (extenderConfiguration.containsBean(PROPERTIES_NAME)) {
				Properties customProperties = (Properties) extenderConfiguration.getBean(PROPERTIES_NAME,
					Properties.class);
				Enumeration propertyKey = customProperties.propertyNames();
				while (propertyKey.hasMoreElements()) {
					String property = (String) propertyKey.nextElement();
					properties.setProperty(property, customProperties.getProperty(property));
				}
			}
		}

		shutdownWaitTime = getShutdownWaitTime(properties);
		processAnnotation = getProcessAnnotations(properties);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Cleanup the configuration items.
	 */
	public void destroy() {

		if (isMulticasterManagedInternally) {
			eventMulticaster.removeAllListeners();
			eventMulticaster = null;
		}

		if (extenderConfiguration != null) {
			extenderConfiguration.close();
			extenderConfiguration = null;
		}

		// postpone the task executor shutdown
		if (forceThreadShutdown && isTaskExecutorManagedInternally) {
			log.warn("Forcing the (internally created) taskExecutor to stop");
			ThreadGroup th = ((SimpleAsyncTaskExecutor) taskExecutor).getThreadGroup();
			if (!th.isDestroyed()) {
				// ask the threads nicely to stop
				th.interrupt();
			}
		}
		taskExecutor = null;
	}

	/**
	 * Copies the URLs returned by the given enumeration and returns them as an
	 * array of Strings for consumption by the application context.
	 * 
	 * @param enm
	 * @return
	 */
	private String[] copyEnumerationToList(Enumeration enm) {
		List urls = new ArrayList(4);
		while (enm != null && enm.hasMoreElements()) {
			URL configURL = (URL) enm.nextElement();
			String configURLAsString = configURL.toExternalForm();
			try {
				urls.add(URLDecoder.decode(configURLAsString, "UTF8"));
			}
			catch (UnsupportedEncodingException uee) {
				log.warn("UTF8 encoding not supported, using the platform default");
				urls.add(URLDecoder.decode(configURLAsString));
			}
		}

		return (String[]) urls.toArray(new String[urls.size()]);
	}

	private Properties createDefaultProperties() {
		Properties properties = new Properties();
		properties.setProperty(SHUTDOWN_WAIT_KEY, "" + DEFAULT_SHUTDOWN_WAIT);
		properties.setProperty(PROCESS_ANNOTATIONS_KEY, "" + DEFAULT_PROCESS_ANNOTATION);

		return properties;
	}

	private TaskExecutor createDefaultTaskExecutor() {
		// create thread-pool for starting contexts
		ThreadGroup threadGroup = new ThreadGroup("spring-osgi-extender[" + ObjectUtils.getIdentityHexString(this)
				+ "]-threads");
		threadGroup.setDaemon(false);

		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setThreadGroup(threadGroup);
		taskExecutor.setThreadNamePrefix("SpringOsgiExtenderThread-");

		isTaskExecutorManagedInternally = true;

		return taskExecutor;
	}

	private OsgiBundleApplicationContextEventMulticaster createDefaultEventMulticaster() {
		return new OsgiBundleApplicationContextEventMulticasterAdapter(new SimpleApplicationEventMulticaster());
	}

	private OsgiApplicationContextCreator createDefaultApplicationContextCreator() {
		return new DefaultOsgiApplicationContextCreator();
	}

	private long getShutdownWaitTime(Properties properties) {
		return Long.parseLong(properties.getProperty(SHUTDOWN_WAIT_KEY));
	}

	private boolean getProcessAnnotations(Properties properties) {
		return Boolean.valueOf(properties.getProperty(PROCESS_ANNOTATIONS_KEY)).booleanValue();
	}

	/**
	 * Returns the taskExecutor.
	 * 
	 * @return Returns the taskExecutor
	 */
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Returns the shutdownWaitTime.
	 * 
	 * @return Returns the shutdownWaitTime
	 */
	public long getShutdownWaitTime() {
		return shutdownWaitTime;
	}

	/**
	 * Indicates if the process annotation is enabled or not.
	 * 
	 * @return Returns true if the annotation should be processed or not
	 * otherwise.
	 */
	public boolean shouldProcessAnnotation() {
		return processAnnotation;
	}

	/**
	 * Returns the eventMulticaster.
	 * 
	 * @return Returns the eventMulticaster
	 */
	public OsgiBundleApplicationContextEventMulticaster getEventMulticaster() {
		return eventMulticaster;
	}

	/**
	 * Sets the flag to force the taskExtender to close up in case of runaway
	 * threads - this applies *only* if the taskExecutor has been created
	 * internally.
	 * 
	 * <p/> The flag will cause a best attempt to shutdown the threads.
	 * 
	 * @param forceThreadShutdown The forceThreadShutdown to set.
	 * @deprecated
	 */
	public void setForceThreadShutdown(boolean forceThreadShutdown) {
		this.forceThreadShutdown = forceThreadShutdown;
	}

	/**
	 * Returns the contextCreator.
	 * 
	 * @return Returns the contextCreator
	 */
	public OsgiApplicationContextCreator getContextCreator() {
		return contextCreator;
	}

	/**
	 * Returns the postProcessors.
	 * 
	 * @return Returns the postProcessors
	 */
	public List getPostProcessors() {
		return postProcessors;
	}

	/**
	 * Returns the class loader wrapped around the extender bundle.
	 * 
	 * @return extender bundle class loader
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}
}
