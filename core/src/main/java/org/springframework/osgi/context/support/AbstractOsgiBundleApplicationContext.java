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

import java.beans.PropertyEditor;
import java.io.IOException;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.support.internal.OsgiBundleScope;
import org.springframework.osgi.io.OsgiBundleResource;
import org.springframework.osgi.io.OsgiBundleResourceLoader;
import org.springframework.osgi.io.OsgiBundleResourcePatternResolver;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.util.internal.MapBasedDictionary;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
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
 * class space, bundle space or jar space).
 * 
 * <p>
 * In addition to the special beans detected by AbstractApplicationContext, this
 * class registers the <code>BundleContextAwareProcessor</code> for processing
 * beans that implement the <code>BundleContextAware</code> interface.
 * 
 * <p>
 * This application context offers the OSGi-specific, "bundle" scope.
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
public abstract class AbstractOsgiBundleApplicationContext extends AbstractRefreshableApplicationContext implements
		ConfigurableOsgiBundleApplicationContext {

	/** OSGi bundle - determined from the BundleContext */
	private Bundle bundle;

	/** OSGi bundle context */
	private BundleContext bundleContext;

	/** Path to configuration files * */
	private String[] configLocations;

	/**
	 * Internal ResourceLoader implementation used for delegation.
	 * 
	 * Note that the PatternResolver is handled through
	 * {@link #getResourcePatternResolver()}
	 */
	private OsgiBundleResourceLoader osgiResourceLoader;

	/**
	 * Internal pattern resolver. The parent one can't be used since it is being
	 * instantiated inside the constructor when the bundle field is not
	 * instantiated yet.
	 */
	private OsgiBundleResourcePatternResolver osgiPatternResolver;

	/** Used for publishing the app context * */
	private ServiceRegistration serviceRegistration;

	/** Should context be published as an OSGi service? */
	private boolean publishContextAsService = true;

	/**
	 * Create a new AbstractOsgiBundleApplicationContext with no parent.
	 */
	public AbstractOsgiBundleApplicationContext() {
		super();
		setDisplayName("Root OsgiBundleApplicationContext");
	}

	/**
	 * Create a new AbstractOsgiBundleApplicationContext with the given parent
	 * context.
	 * 
	 * @param parent the parent context
	 */
	public AbstractOsgiBundleApplicationContext(ApplicationContext parent) {
		super(parent);
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
		this.osgiPatternResolver = new OsgiBundleResourcePatternResolver(this.bundle);
		this.setClassLoader(createBundleClassLoader(this.bundle));
		this.setDisplayName(ClassUtils.getShortName(getClass()) + "(bundle=" + getBundleSymbolicName() + ", config="
				+ StringUtils.arrayToCommaDelimitedString(getConfigLocations()) + ")");
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
		this.configLocations = configLocations;
	}

	/**
	 * Return this application context configuration locations. The default
	 * implementation will check whether there are any locations configured and,
	 * if not, will return the default locations.
	 * 
	 * @see #getDefaultConfigLocations()
	 * @return application context configuration locations.
	 */
	public String[] getConfigLocations() {
		return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
	}

	/**
	 * Unregister the ApplicationContext OSGi service (in case there is any).
	 */
	protected void doClose() {
		if (!OsgiServiceUtils.unregisterService(serviceRegistration)) {
			logger.info("the application context service has been already unregistered");
			serviceRegistration = null;
		}

		// call super class
		super.doClose();
	}

	/**
	 * Clean up any beans from the bundle scope.
	 */
	protected void destroyBeans() {
		super.destroyBeans();

		try {
			cleanOsgiBundleScope(getBeanFactory());
		}
		catch (Exception ex) {
			logger.warn("got exception when closing", ex);
		}
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

	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		super.postProcessBeanFactory(beanFactory);

		beanFactory.addBeanPostProcessor(new BundleContextAwareProcessor(this.bundleContext));
		beanFactory.ignoreDependencyInterface(BundleContextAware.class);

		// add bundleContext bean
		if (!beanFactory.containsLocalBean(BUNDLE_CONTEXT_BEAN_NAME)) {
			logger.debug("registering BundleContext as a bean named " + BUNDLE_CONTEXT_BEAN_NAME);
			beanFactory.registerSingleton(BUNDLE_CONTEXT_BEAN_NAME, this.bundleContext);
		}
		else {
			logger.warn("a bean named " + BUNDLE_CONTEXT_BEAN_NAME
					+ " already exists; the bundleContext will not be registered as a bean");
		}

		// register property editors
		registerPropertyEditors(beanFactory);

		// register a 'bundle' scope
		beanFactory.registerScope(OsgiBundleScope.SCOPE_NAME, new OsgiBundleScope());
	}

	/**
	 * Register OSGi-specific {@link PropertyEditor}s.
	 * 
	 * @param beanFactory beanFactory used for registration.
	 */
	protected void registerPropertyEditors(ConfigurableListableBeanFactory beanFactory) {
		beanFactory.addPropertyEditorRegistrar(new OsgiPropertyEditorRegistrar(getClassLoader()));
	}

	protected void cleanOsgiBundleScope(ConfigurableListableBeanFactory beanFactory) {
		Scope scope = beanFactory.getRegisteredScope(OsgiBundleScope.SCOPE_NAME);
		if (scope != null && scope instanceof OsgiBundleScope) {
			if (logger.isDebugEnabled())
				logger.debug("destroying existing bundle scope beans...");
			((OsgiBundleScope) scope).destroy();
		}
	}

	/**
	 * Publish the application context as an OSGi service. The method internally
	 * takes care of parsing the bundle headers and determined if actual
	 * publishing is required or not.
	 * 
	 */
	protected void publishContextAsOsgiServiceIfNecessary() {
		if (publishContextAsService) {
			Dictionary serviceProperties = new MapBasedDictionary();
			serviceProperties.put(APPLICATION_CONTEXT_SERVICE_PROPERTY_NAME, getBundleSymbolicName());
			if (logger.isInfoEnabled()) {
				logger.info("Publishing application context with properties ("
						+ APPLICATION_CONTEXT_SERVICE_PROPERTY_NAME + "=" + getBundleSymbolicName() + ")");
			}

			Class[] classes = org.springframework.osgi.util.internal.ClassUtils.getClassHierarchy(getClass(),
				org.springframework.osgi.util.internal.ClassUtils.INCLUDE_ALL_CLASSES);

			// filter classes based on visibility
			Class[] filterClasses = org.springframework.osgi.util.internal.ClassUtils.getVisibleClasses(classes,
				this.getClass().getClassLoader());

			String[] serviceNames = org.springframework.osgi.util.internal.ClassUtils.toStringArray(filterClasses);

			if (logger.isDebugEnabled())
				logger.debug("publishing service under classes " + ObjectUtils.nullSafeToString(serviceNames));

			// Publish under all the significant interfaces we see
			this.serviceRegistration = getBundleContext().registerService(serviceNames, this, serviceProperties);
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Not publishing application context with properties ("
						+ APPLICATION_CONTEXT_SERVICE_PROPERTY_NAME + "=" + getBundleSymbolicName() + ")");
			}
		}
	}

	public String getBundleSymbolicName() {
		return OsgiStringUtils.nullSafeSymbolicName(getBundle());
	}

	/**
	 * This implementation supports pattern matching inside the OSGi bundle.
	 * 
	 * @see OsgiBundleResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new OsgiBundleResourcePatternResolver(this.bundle);
	}

	// delegate methods to a proper OsgiResourceLoader

	public ClassLoader getClassLoader() {
		return osgiResourceLoader.getClassLoader();
	}

	public Resource getResource(String location) {
		return osgiResourceLoader.getResource(location);
	}

	public Resource[] getResources(String locationPattern) throws IOException {
		return osgiPatternResolver.getResources(locationPattern);
	}

	public void setClassLoader(ClassLoader classLoader) {
		osgiResourceLoader.setClassLoader(classLoader);
	}

	protected Resource getResourceByPath(String path) {
		Assert.notNull(path, "Path is required");
		return new OsgiBundleResource(this.bundle, path);
	}

	public void setPublishContextAsService(boolean publishContextAsService) {
		this.publishContextAsService = publishContextAsService;
	}

	/**
	 * Create the classloader that delegates to the underlying OSGi bundle.
	 * 
	 * @param bundle
	 * @return
	 */
	protected ClassLoader createBundleClassLoader(Bundle bundle) {
		return BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, ProxyFactory.class.getClassLoader());
	}
}
