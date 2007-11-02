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
 *
 */
package org.springframework.osgi.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Install Bundles using a FactoryBean.
 * <p/>
 * This allows customers to use Spring to drive bundle management. Bundles
 * states can be modified using the state parameter. Most commonly this is set
 * to "start".
 * <p/>
 * <p/>Pay attention when installing bundles dynamically since classes can be
 * loaded aggressively.
 *
 * @author Andy Piper
 */
public class BundleFactoryBean implements FactoryBean, BundleContextAware, InitializingBean, DisposableBean,
		ResourceLoaderAware {

	private static Log log = LogFactory.getLog(BundleFactoryBean.class);

	public static final String START_ACTION = "start";

	public static final String STOP_ACTION = "stop";

	public static final String INSTALL_ACTION = "install";

	public static final String UNINSTALL_ACTION = "uninstall";

	public static final String UPDATE_ACTION = "update";

	// bundle info

	/**
	 * Bundle location
	 */
	protected String location;

	protected Resource resource;

	protected ResourceLoader resourceLoader;

	/**
	 * Bundle symName
	 */
	protected String symbolicName;

	/**
	 * Actual bundle
	 */
	private Bundle bundle;

	protected BundleContext bundleContext;

	private String action, destroyAction;

	private int startLevel;

	private ClassLoader classloader;

	/**
	 * unused at the moment
	 */
	private boolean pushBundleAsContextClassLoader = false;

	/**
	 * wait until the operation invoked completes
	 */
	private boolean waitToComplete = false;

	// FactoryBean methods
	public Class getObjectType() {
		return (bundle != null ? bundle.getClass() : Bundle.class);
	}

	public boolean isSingleton() {
		return true;
	}

	public Object getObject() throws Exception {
		return bundle;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(bundleContext, "BundleContext is required");

		// check parameters
		if (symbolicName == null && !StringUtils.hasText(location) && resource == null)
			throw new IllegalArgumentException("Bundle-SymbolicName or location required");

		if (StringUtils.hasText(location)) {
			resource = resourceLoader.getResource(location);
		} else {
			location = resource.getURL().toString();
		}

		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		try {
			if (pushBundleAsContextClassLoader) {
				Thread.currentThread().setContextClassLoader(classloader);
			}

			// find the bundle first of all
			bundle = findBundle();

			executeAction(action);
		}
		catch (BundleException ex) {
			log.error(ex.getMessage(), ex);
			throw (RuntimeException) new IllegalArgumentException("bundle "
					+ OsgiStringUtils.nullSafeNameAndSymName(bundle) + " threw exception ").initCause(ex);
		}
		finally {
			if (pushBundleAsContextClassLoader) {
				Thread.currentThread().setContextClassLoader(ccl);
			}
		}
	}

	public void destroy() throws Exception {
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		try {
			if (pushBundleAsContextClassLoader) {
				Thread.currentThread().setContextClassLoader(classloader);
			}

			executeAction(destroyAction);
		} finally {
			if (pushBundleAsContextClassLoader) {
				Thread.currentThread().setContextClassLoader(ccl);
			}
		}

		bundle = null;
		classloader = null;
	}

	/**
	 * Find a bundle based on the configuration.
	 *
	 * @return a Bundle instance based on the configuration.
	 * @throws Exception
	 */
	private Bundle findBundle() throws Exception {
		// first consider symbolicName
		Bundle bundle = null;

		if (symbolicName != null) {
			bundle = OsgiBundleUtils.findBundleBySymbolicName(bundleContext, symbolicName);
		}

		// no bundle (or no symbolicName)
		if (bundle == null) {
			// install bundle (default)
			log.info("Loading bundle [" + location + "]");
			// TODO: consider resource
			bundle = bundleContext.installBundle(location);


			Assert.notNull(location, "could not find an installed bundle with symbolicName=[" + symbolicName
					+ "] and no location was specified; cannot continue");

		}

		// TODO: keep the start-level or not?
		// updateStartLevel(getStartLevel());

		return bundle;
	}

	protected void executeAction(String action) throws Exception {
		// default action (or no bundle) requires installation
		if (bundle == null || INSTALL_ACTION.equalsIgnoreCase(action)) {
			bundleContext.installBundle(location, resource.getInputStream());

			if (symbolicName == null) {
				symbolicName = bundle.getSymbolicName();
			}

			// TODO: uncomment this
			// do validation (is the specified symbolic name correct?)
//			else {
//				String installedSymName = bundle.getSymbolicName();
//				Assert.isTrue(symbolicName.equals(installedSymName),
//					"the install bundle has a different symbolicName [" + installedSymName
//							+ "] then the specified symbolicName=" + symbolicName);
//			}

		} else if (START_ACTION.equalsIgnoreCase(action)) {
			bundle.start();
		} else if (STOP_ACTION.equalsIgnoreCase(action)) {
			bundle.stop();
		} else if (UPDATE_ACTION.equalsIgnoreCase(action)) {
			bundle.update();
		} else if (UNINSTALL_ACTION.equalsIgnoreCase(action)) {
			bundle.uninstall();
		}
	}

	public void setLocation(String location) throws Exception {
		this.location = location;
	}

	public void setLocation(Resource resource) throws Exception {
		this.resource = resource;
	}

	public Resource getResource() {
		return this.resource;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public ClassLoader getClassloader() {
		return classloader;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public String getAction() {
		return action;
	}

	public String getLocation() {
		return location;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public void setBundleContext(BundleContext context) {
		bundleContext = context;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setDestroyAction(String action) {
		this.destroyAction = action;
	}

	public void setStartLevel(int startLevel) {
		this.startLevel = startLevel;
	}

	/**
	 * Determines whether invocations on the remote service should be performed
	 * in the context of the target bundle's ClassLoader. The default is false.
	 *
	 * @param pushBundleAsContextClassLoader
	 */
	public void setPushBundleAsContextClassloader(boolean pushBundleAsContextClassLoader) {
		this.pushBundleAsContextClassLoader = pushBundleAsContextClassLoader;
	}

	public void setClassloader(ClassLoader classloader) {
		this.classloader = classloader;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}


	// TODO: we don't support start-levels yet
	private void updateStartLevel(int level) {
		if (level == 0 || bundle == null)
			return;
		// Set the start level of the bundle if we are able.
		ServiceReference startref = bundleContext.getServiceReference(StartLevel.class.getName());
		if (startref != null) {
			StartLevel start = (StartLevel) bundleContext.getService(startref);
			if (start != null) {
				start.setBundleStartLevel(bundle, level);
			}
			bundleContext.ungetService(startref);
		}
	}

}
