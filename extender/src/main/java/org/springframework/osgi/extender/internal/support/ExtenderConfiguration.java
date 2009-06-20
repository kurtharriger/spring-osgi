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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticaster;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticasterAdapter;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.extender.OsgiApplicationContextCreator;
import org.springframework.osgi.extender.OsgiBeanFactoryPostProcessor;
import org.springframework.osgi.extender.OsgiServiceDependencyFactory;
import org.springframework.osgi.extender.internal.dependencies.startup.MandatoryImporterDependencyFactory;
import org.springframework.osgi.extender.support.DefaultOsgiApplicationContextCreator;
import org.springframework.osgi.extender.support.internal.ConfigUtils;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.springframework.scheduling.timer.TimerTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Configuration class for the extender. Takes care of locating the extender specific configurations and merging the
 * results with the defaults.
 * 
 * @author Costin Leau
 */
public class ExtenderConfiguration implements DisposableBean {

	/** logger */
	private final Log log;

	private static final String TASK_EXECUTOR_NAME = "taskExecutor";

	private static final String SHUTDOWN_TASK_EXECUTOR_NAME = "shutdownTaskExecutor";

	private static final String CONTEXT_CREATOR_NAME = "applicationContextCreator";

	private static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "osgiApplicationEventMulticaster";

	private static final String CONTEXT_LISTENER_NAME = "osgiApplicationContextListener";

	private static final String PROPERTIES_NAME = "extenderProperties";

	private static final String SHUTDOWN_WAIT_KEY = "shutdown.wait.time";

	private static final String PROCESS_ANNOTATIONS_KEY = "process.annotations";

	private static final String WAIT_FOR_DEPS_TIMEOUT_KEY = "dependencies.wait.time";

	private static final String EXTENDER_CFG_LOCATION = "META-INF/spring/extender";

	private static final String XML_PATTERN = "*.xml";

	private static final String ANNOTATION_DEPENDENCY_FACTORY =
			"org.springframework.osgi.extensions.annotation.ServiceReferenceDependencyBeanFactoryPostProcessor";

	/** annotation processing system property (kept for backwards compatibility) */
	private static final String AUTO_ANNOTATION_PROCESSING =
			"org.springframework.osgi.extender.annotation.auto.processing";

	//
	// defaults
	//

	// default dependency wait time (in milliseconds)
	private static final long DEFAULT_DEP_WAIT = ConfigUtils.DIRECTIVE_TIMEOUT_DEFAULT * 1000;
	private static final long DEFAULT_SHUTDOWN_WAIT = 60 * 1000;
	private static final boolean DEFAULT_PROCESS_ANNOTATION = false;

	private ConfigurableOsgiBundleApplicationContext extenderConfiguration;

	private TaskExecutor taskExecutor, shutdownTaskExecutor;

	private boolean isTaskExecutorManagedInternally;

	private boolean isShutdownTaskExecutorManagedInternally;

	private boolean isMulticasterManagedInternally;

	private long shutdownWaitTime, dependencyWaitTime;

	private boolean processAnnotation;

	private OsgiBundleApplicationContextEventMulticaster eventMulticaster;

	private OsgiBundleApplicationContextListener contextEventListener;

	private boolean forceThreadShutdown;

	private OsgiApplicationContextCreator contextCreator;

	/** bundle wrapped class loader */
	private final ClassLoader classLoader;
	/** List of context post processors */
	private final List<OsgiBeanFactoryPostProcessor> postProcessors =
			Collections.synchronizedList(new ArrayList<OsgiBeanFactoryPostProcessor>(0));
	/** List of service dependency factories */
	private final List<OsgiServiceDependencyFactory> dependencyFactories =
			Collections.synchronizedList(new ArrayList<OsgiServiceDependencyFactory>(0));

	// fields reading/writing lock
	private final Object lock = new Object();

	/**
	 * Constructs a new <code>ExtenderConfiguration</code> instance. Locates the extender configuration, creates an
	 * application context which will returned the extender items.
	 * 
	 * @param bundleContext extender OSGi bundle context
	 */
	public ExtenderConfiguration(BundleContext bundleContext, Log log) {
		this.log = log;
		Bundle bundle = bundleContext.getBundle();
		Properties properties = new Properties(createDefaultProperties());

		Enumeration<?> enm = bundle.findEntries(EXTENDER_CFG_LOCATION, XML_PATTERN, false);

		if (enm == null) {
			log.info("No custom extender configuration detected; using defaults...");

			synchronized (lock) {
				taskExecutor = createDefaultTaskExecutor();
				shutdownTaskExecutor = createDefaultShutdownTaskExecutor();
				eventMulticaster = createDefaultEventMulticaster();
				contextCreator = createDefaultApplicationContextCreator();
				contextEventListener = createDefaultApplicationContextListener();

			}
			classLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle);
		} else {
			String[] configs = copyEnumerationToList(enm);

			log.info("Detected extender custom configurations at " + ObjectUtils.nullSafeToString(configs));
			// create OSGi specific XML context
			ConfigurableOsgiBundleApplicationContext extenderAppCtx = new OsgiBundleXmlApplicationContext(configs);
			extenderAppCtx.setBundleContext(bundleContext);
			extenderAppCtx.refresh();

			synchronized (lock) {
				extenderConfiguration = extenderAppCtx;
				// initialize beans
				taskExecutor =
						extenderConfiguration.containsBean(TASK_EXECUTOR_NAME) ? (TaskExecutor) extenderConfiguration
								.getBean(TASK_EXECUTOR_NAME, TaskExecutor.class) : createDefaultTaskExecutor();

				shutdownTaskExecutor =
						extenderConfiguration.containsBean(SHUTDOWN_TASK_EXECUTOR_NAME) ? (TaskExecutor) extenderConfiguration
								.getBean(SHUTDOWN_TASK_EXECUTOR_NAME, TaskExecutor.class)
								: createDefaultShutdownTaskExecutor();

				eventMulticaster =
						extenderConfiguration.containsBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME) ? (OsgiBundleApplicationContextEventMulticaster) extenderConfiguration
								.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
										OsgiBundleApplicationContextEventMulticaster.class)
								: createDefaultEventMulticaster();

				contextCreator =
						extenderConfiguration.containsBean(CONTEXT_CREATOR_NAME) ? (OsgiApplicationContextCreator) extenderConfiguration
								.getBean(CONTEXT_CREATOR_NAME, OsgiApplicationContextCreator.class)
								: createDefaultApplicationContextCreator();

				contextEventListener =
						extenderConfiguration.containsBean(CONTEXT_LISTENER_NAME) ? (OsgiBundleApplicationContextListener) extenderConfiguration
								.getBean(CONTEXT_LISTENER_NAME, OsgiBundleApplicationContextListener.class)
								: createDefaultApplicationContextListener();
			}

			// get post processors
			postProcessors.addAll(extenderConfiguration.getBeansOfType(OsgiBeanFactoryPostProcessor.class).values());

			// get dependency factories
			dependencyFactories.addAll(extenderConfiguration.getBeansOfType(OsgiServiceDependencyFactory.class)
					.values());

			classLoader = extenderConfiguration.getClassLoader();
			// extender properties using the defaults as backup
			if (extenderConfiguration.containsBean(PROPERTIES_NAME)) {
				Properties customProperties =
						(Properties) extenderConfiguration.getBean(PROPERTIES_NAME, Properties.class);
				Enumeration<?> propertyKey = customProperties.propertyNames();
				while (propertyKey.hasMoreElements()) {
					String property = (String) propertyKey.nextElement();
					properties.setProperty(property, customProperties.getProperty(property));
				}
			}
		}

		synchronized (lock) {
			shutdownWaitTime = getShutdownWaitTime(properties);
			dependencyWaitTime = getDependencyWaitTime(properties);
			processAnnotation = getProcessAnnotations(properties);
		}

		// load default dependency factories
		addDefaultDependencyFactories();

		// allow post processing
		contextCreator = postProcess(contextCreator);
	}

	/**
	 * Allows post processing of the context creator.
	 * 
	 * @param contextCreator
	 * @return
	 */
	protected OsgiApplicationContextCreator postProcess(OsgiApplicationContextCreator contextCreator) {
		return contextCreator;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Cleanup the configuration items.
	 */
	public void destroy() {

		synchronized (lock) {
			if (isMulticasterManagedInternally) {
				eventMulticaster.removeAllListeners();
				eventMulticaster = null;
			}

			if (extenderConfiguration != null) {
				extenderConfiguration.close();
				extenderConfiguration = null;
			}

			// postpone the task executor shutdown
			if (forceThreadShutdown) {

				if (isTaskExecutorManagedInternally) {
					log.warn("Forcing the (internally created) taskExecutor to stop...");
					ThreadGroup th = ((SimpleAsyncTaskExecutor) taskExecutor).getThreadGroup();
					if (!th.isDestroyed()) {
						// ask the threads nicely to stop
						th.interrupt();
					}
				}
				taskExecutor = null;
			}

			if (isShutdownTaskExecutorManagedInternally) {
				try {
					((DisposableBean) shutdownTaskExecutor).destroy();
				} catch (Exception ex) {
					log.debug("Received exception while shutting down shutdown task executor", ex);
				}
				shutdownTaskExecutor = null;
			}
		}
	}

	/**
	 * Copies the URLs returned by the given enumeration and returns them as an array of Strings for consumption by the
	 * application context.
	 * 
	 * @param enm
	 * @return
	 */
	@SuppressWarnings("deprecation")
	private String[] copyEnumerationToList(Enumeration<?> enm) {
		List<String> urls = new ArrayList<String>(4);
		while (enm != null && enm.hasMoreElements()) {
			URL configURL = (URL) enm.nextElement();
			if (configURL != null) {
				String configURLAsString = configURL.toExternalForm();
				try {
					urls.add(URLDecoder.decode(configURLAsString, "UTF8"));
				} catch (UnsupportedEncodingException uee) {
					log.warn("UTF8 encoding not supported, using the platform default");
					urls.add(URLDecoder.decode(configURLAsString));
				}
			}
		}

		return (String[]) urls.toArray(new String[urls.size()]);
	}

	private Properties createDefaultProperties() {
		Properties properties = new Properties();
		properties.setProperty(SHUTDOWN_WAIT_KEY, "" + DEFAULT_SHUTDOWN_WAIT);
		properties.setProperty(PROCESS_ANNOTATIONS_KEY, "" + DEFAULT_PROCESS_ANNOTATION);
		properties.setProperty(WAIT_FOR_DEPS_TIMEOUT_KEY, "" + DEFAULT_DEP_WAIT);

		return properties;
	}

	protected void addDefaultDependencyFactories() {
		boolean debug = log.isDebugEnabled();

		// default JDK 1.4 processor
		dependencyFactories.add(0, new MandatoryImporterDependencyFactory());

		// load through reflection the dependency and injection processors if running on JDK 1.5 and annotation
		// processing is enabled
		if (processAnnotation) {
			// dependency processor
			Class<?> annotationProcessor = null;
			try {
				annotationProcessor =
						Class.forName(ANNOTATION_DEPENDENCY_FACTORY, false, ExtenderConfiguration.class
								.getClassLoader());
			} catch (ClassNotFoundException cnfe) {
				log.warn("Spring DM annotation package not found, annotation processing disabled.", cnfe);
				return;
			}
			Object processor = BeanUtils.instantiateClass(annotationProcessor);
			Assert.isInstanceOf(OsgiServiceDependencyFactory.class, processor);
			dependencyFactories.add(1, (OsgiServiceDependencyFactory) processor);

			if (debug)
				log.debug("Succesfully loaded annotation dependency processor [" + ANNOTATION_DEPENDENCY_FACTORY + "]");

			// add injection processor (first in line)
			postProcessors.add(0, new OsgiAnnotationPostProcessor());
			log.info("Spring-DM annotation processing enabled");
		} else {
			if (debug) {
				log.debug("Spring-DM annotation processing disabled; [" + ANNOTATION_DEPENDENCY_FACTORY
						+ "] not loaded");
			}
		}

	}

	private TaskExecutor createDefaultTaskExecutor() {
		// create thread-pool for starting contexts
		ThreadGroup threadGroup =
				new ThreadGroup("spring-osgi-extender[" + ObjectUtils.getIdentityHexString(this) + "]-threads");
		threadGroup.setDaemon(false);

		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setThreadGroup(threadGroup);
		taskExecutor.setThreadNamePrefix("SpringOsgiExtenderThread-");

		isTaskExecutorManagedInternally = true;

		return taskExecutor;
	}

	private TaskExecutor createDefaultShutdownTaskExecutor() {
		TimerTaskExecutor taskExecutor = new TimerTaskExecutor();
		taskExecutor.afterPropertiesSet();
		isShutdownTaskExecutorManagedInternally = true;
		return taskExecutor;
	}

	private OsgiBundleApplicationContextEventMulticaster createDefaultEventMulticaster() {
		isMulticasterManagedInternally = true;
		return new OsgiBundleApplicationContextEventMulticasterAdapter(new SimpleApplicationEventMulticaster());
	}

	private OsgiApplicationContextCreator createDefaultApplicationContextCreator() {
		return new DefaultOsgiApplicationContextCreator();
	}

	private OsgiBundleApplicationContextListener createDefaultApplicationContextListener() {
		return new DefaultOsgiBundleApplicationContextListener(log);
	}

	private long getShutdownWaitTime(Properties properties) {
		return Long.parseLong(properties.getProperty(SHUTDOWN_WAIT_KEY));
	}

	private long getDependencyWaitTime(Properties properties) {
		return Long.parseLong(properties.getProperty(WAIT_FOR_DEPS_TIMEOUT_KEY));
	}

	private boolean getProcessAnnotations(Properties properties) {
		return Boolean.valueOf(properties.getProperty(PROCESS_ANNOTATIONS_KEY)).booleanValue()
				|| Boolean.getBoolean(AUTO_ANNOTATION_PROCESSING);
	}

	/**
	 * Returns the taskExecutor.
	 * 
	 * @return Returns the taskExecutor
	 */
	public TaskExecutor getTaskExecutor() {
		synchronized (lock) {
			return taskExecutor;
		}
	}

	/**
	 * Returns the shutdown task executor.
	 * 
	 * @return Returns the shutdown task executor
	 */
	public TaskExecutor getShutdownTaskExecutor() {
		synchronized (lock) {
			return shutdownTaskExecutor;
		}
	}

	/**
	 * Returns the contextEventListener.
	 * 
	 * @return Returns the contextEventListener
	 */
	public OsgiBundleApplicationContextListener getContextEventListener() {
		synchronized (lock) {
			return contextEventListener;
		}
	}

	/**
	 * Returns the shutdownWaitTime.
	 * 
	 * @return Returns the shutdownWaitTime
	 */
	public long getShutdownWaitTime() {
		synchronized (lock) {
			return shutdownWaitTime;
		}
	}

	/**
	 * Indicates if the process annotation is enabled or not.
	 * 
	 * @return Returns true if the annotation should be processed or not otherwise.
	 */
	public boolean shouldProcessAnnotation() {
		synchronized (lock) {
			return processAnnotation;
		}
	}

	/**
	 * Returns the dependencyWaitTime.
	 * 
	 * @return Returns the dependencyWaitTime
	 */
	public long getDependencyWaitTime() {
		synchronized (lock) {
			return dependencyWaitTime;
		}
	}

	/**
	 * Returns the eventMulticaster.
	 * 
	 * @return Returns the eventMulticaster
	 */
	public OsgiBundleApplicationContextEventMulticaster getEventMulticaster() {
		synchronized (lock) {
			return eventMulticaster;
		}
	}

	/**
	 * Sets the flag to force the taskExtender to close up in case of runaway threads - this applies *only* if the
	 * taskExecutor has been created internally.
	 * 
	 * <p/> The flag will cause a best attempt to shutdown the threads.
	 * 
	 * @param forceThreadShutdown The forceThreadShutdown to set.
	 */
	public void setForceThreadShutdown(boolean forceThreadShutdown) {
		synchronized (lock) {
			this.forceThreadShutdown = forceThreadShutdown;
		}
	}

	/**
	 * Returns the contextCreator.
	 * 
	 * @return Returns the contextCreator
	 */
	public OsgiApplicationContextCreator getContextCreator() {
		synchronized (lock) {
			return contextCreator;
		}
	}

	/**
	 * Returns the postProcessors.
	 * 
	 * @return Returns the postProcessors
	 */
	public List<OsgiBeanFactoryPostProcessor> getPostProcessors() {
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

	/**
	 * Returns the dependencies factories declared by the extender configuration. The list automatically contains the
	 * default listeners (such as the annotation one).
	 * 
	 * @return list of dependency factories
	 */
	public List<OsgiServiceDependencyFactory> getDependencyFactories() {
		return dependencyFactories;
	}
}