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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.io.OsgiBundleResource;
import org.springframework.osgi.io.OsgiBundleResourceLoader;
import org.springframework.osgi.io.OsgiBundleResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 
 * AbstractRefreshableApplicationContext subclass that implements the
 * ConfigurableOsgiApplicationContext interface for OSGi environments.
 * Pre-implements a "configLocation" property, to be populated through the
 * ConfigurableOsgiApplicationContext interface on OSGi bundle startup.
 * 
 * <p>
 * This class is as easy to subclass as AbstractRefreshableApplicationContext:
 * All you need to implement is the <code>loadBeanDefinitions</code> method;
 * see the superclass javadoc for details. Note that implementations are
 * supposed to load bean definitions from the files specified by the locations
 * returned by the <code>getConfigLocations</code> method.
 * 
 * <p>
 * Interprets resource paths as OSGi bundle resources (either from the bundle
 * classpath or OSGi entries).
 * 
 * <p>
 * In addition to the special beans detected by AbstractApplicationContext, this
 * class registers the <code>BundleContextAwareProcessor</code> for processing
 * beans that implement the <code>BundleContextAware</code> interface.
 * 
 * 
 * <p>
 * Note that OsgiApplicationContext implementations are generally supposed to
 * configure themselves based on the configuration received through the
 * ConfigurableOsgiBundleApplicationContext interface. In contrast, a standalone
 * application context might allow for configuration in custom startup code (for
 * example, GenericApplicationContext).
 * 
 * @author Costin Leau
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * 
 */
public abstract class AbstractRefreshableOsgiBundleApplicationContext extends AbstractRefreshableApplicationContext
		implements ConfigurableOsgiBundleApplicationContext {

	/** OSGi bundle - determined from the BundleContext */
	private Bundle bundle;

	/** OSGi bundle context */
	private BundleContext bundleContext;

	/** Path to configuration files * */
	private String[] configLocations;

	/** Internal ResourceLoader implementation used for delegation * */
	private OsgiBundleResourceLoader osgiResourceLoader;

	public AbstractRefreshableOsgiBundleApplicationContext() {
		super(null);
		setDisplayName("Root OsgiBundleApplicationContext");
	}

	/**
	 * Set the bundleContext for this application context. Will automatically
	 * determine the bundle, create a new ResourceLoader (and set its
	 * classloader to a custom implementation that will delegate the calls to
	 * the bundle).
	 */
	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		this.bundle = bundleContext.getBundle();
		this.osgiResourceLoader = new OsgiBundleResourceLoader(this.bundle);
		this.setClassLoader(createBundleClassLoader(this.bundle));
	}

	/**
	 * Get the OSGi BundleContext for this application context
	 */
	public BundleContext getBundleContext() {
		return this.bundleContext;
	}

	public Bundle getBundle() {
		return this.bundle;
	}

	public void setConfigLocations(String[] configLocations) {
		this.configLocations = defensiveCopyOf(configLocations);
	}

	protected String[] getConfigLocations() {
		return defensiveCopyOf(this.configLocations);
	}
	
	protected boolean inActiveBundleState() {
		int state = getBundle().getState();
		return (state == Bundle.ACTIVE || state == Bundle.STARTING);
	}
	
	protected String bundleStateName() {
		int state = getBundle().getState();
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		default:
			return "IN UNKNOWN STATE";
		}
	}
	private String[] defensiveCopyOf(String[] original) {
		if (null == original) {
			return new String[0];
		}
		else {
			String[] ret = new String[original.length];
			System.arraycopy(original,0,ret,0,original.length);
			return ret;
		}
	}

	/**
	 * Sets a default config location if no explicit config location specified.
	 * Synchronization required to cope with dynamic nature of OSGi which may
	 * attempt to close this bundle during the refresh!
	 * 
	 * @see #getDefaultConfigLocations
	 * @see #setConfigLocations
	 */
	public void refresh() throws BeansException {
		if (this.configLocations == null || this.configLocations.length == 0) {
			setConfigLocations(getDefaultConfigLocations());
		}
		synchronized (this) {
			if (!inActiveBundleState()) {
				throw new ApplicationContextException("Unable to refresh application context: bundle is " + 
											bundleStateName());
			}
			super.refresh();
		}
	}

	// synchronization required around close as after this, the BundleContext is
	// invalid - cannot allow close to happen on a separate thread during refresh!
	public synchronized void close() {
		super.close();
	}
	
	/**
	 * Return the default config locations to use, for the case where no
	 * explicit config locations have been specified.
	 * <p>
	 * Default implementation returns null, requiring explicit config locations.
	 * 
	 * @see #setConfigLocations
	 */
	protected String[] getDefaultConfigLocations() {
		return null;
	}

	/**
	 * Register post processor for BeanContextAware beans.
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		super.postProcessBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(new BundleContextAwareProcessor(this.bundleContext));
		beanFactory.ignoreDependencyInterface(BundleContextAware.class);
	}

	/**
	 * This implementation supports pattern matching inside the OSGi bundle.
	 * 
	 * @see OsgiBundleResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new OsgiBundleResourcePatternResolver(this);
	}

	// delegate methods to a proper OsgiResourceLoader

	public ClassLoader getClassLoader() {
		return osgiResourceLoader.getClassLoader();
	}

	public Resource getResource(String location) {
		return osgiResourceLoader.getResource(location);
	}

	public void setClassLoader(ClassLoader classLoader) {
		osgiResourceLoader.setClassLoader(classLoader);
	}

	protected Resource getResourceByPath(String path) {
		Assert.notNull(path, "Path is required");
		return new OsgiBundleResource(this.bundle, path);
	}

	/**
	 * Create the classloader that delegates to the underlying OSGi bundle.
	 * 
	 * @param bundle
	 * @return
	 */
	protected ClassLoader createBundleClassLoader(Bundle bundle) {
		return BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle);
	}

	/**
	 * Return diagnostic information.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer(super.toString());
		sb.append("; ");
		sb.append("config locations [");
		sb.append(StringUtils.arrayToCommaDelimitedString(this.configLocations));
		sb.append("]");
		return sb.toString();
	}

}
