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

package org.springframework.osgi.web.context.support;

import java.lang.reflect.Method;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.context.support.OsgiBundleXmlApplicationContext;
import org.springframework.osgi.io.OsgiBundleResourceLoader;
import org.springframework.osgi.web.extender.deployer.WarDeploymentContext;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;
import org.springframework.ui.context.support.UiApplicationContextUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextAwareProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * OSGi variant for {@link XmlWebApplicationContext}. The implementation
 * mandates that the OSGi bundle context is either set ({@link #setBundleContext(BundleContext)}
 * before setting the ServletContext or that the given ServletContext contains
 * the BundleContext as an attribute under
 * {@link WarDeploymentContext#OSGI_BUNDLE_CONTEXT_CONTEXT_ATTRIBUTE}.
 * 
 * <p/> Additionally, this implementation replaces the {@link ServletContext}
 * resource loading with an OSGi specific loader which provides equivalent
 * functionality.
 * 
 * @see XmlWebApplicationContext
 * @see OsgiBundleResourceLoader
 * @see OsgiBundleXmlApplicationContext
 * 
 * @author Costin Leau
 */
public class OsgiBundleXmlWebApplicationContext extends OsgiBundleXmlApplicationContext implements
		ConfigurableWebApplicationContext, ThemeSource {

	/** Servlet context that this context runs in */
	private ServletContext servletContext;

	/** Servlet config that this context runs in, if any */
	private ServletConfig servletConfig;

	/** Namespace of this context, or <code>null</code> if root */
	private String namespace;

	/** the ThemeSource for this ApplicationContext */
	private ThemeSource themeSource;


	public OsgiBundleXmlWebApplicationContext() {
		setDisplayName("Root OsgiWebApplicationContext");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Additionally, if the {@link BundleContext} is not set, it is looked up
	 * under {@link WarDeploymentContext #OSGI_BUNDLE_CONTEXT_CONTEXT_ATTRIBUTE}.
	 */
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;

		// look for the attribute only if there is no BundleContext available
		if (getBundleContext() == null && servletContext != null) {
			Object context = servletContext.getAttribute(WarDeploymentContext.OSGI_BUNDLE_CONTEXT_CONTEXT_ATTRIBUTE);

			Assert.notNull(context, "BundleContext expected as attribute under "
					+ WarDeploymentContext.OSGI_BUNDLE_CONTEXT_CONTEXT_ATTRIBUTE);
			Assert.isInstanceOf(BundleContext.class, context);

			setBundleContext((BundleContext) context);
		}
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}

	public void setServletConfig(ServletConfig servletConfig) {
		this.servletConfig = servletConfig;
		if (servletConfig != null && this.servletContext == null) {
			this.servletContext = servletConfig.getServletContext();
		}
	}

	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
		if (namespace != null) {
			setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
		}
	}

	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * Register request/session scopes, a {@link ServletContextAwareProcessor},
	 * etc.
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		super.postProcessBeanFactory(beanFactory);

		beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
		beanFactory.ignoreDependencyInterface(ServletContextAware.class);
		beanFactory.ignoreDependencyInterface(ServletConfigAware.class);
		beanFactory.registerResolvableDependency(ServletContext.class, this.servletContext);
		beanFactory.registerResolvableDependency(ServletConfig.class, this.servletConfig);

		registerWebApplicationScopes(beanFactory);
	}

	// TODO: remove ugly hack
	private void registerWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
		Method method = ReflectionUtils.findMethod(WebApplicationContextUtils.class, "registerWebApplicationScopes",
			new Class[] { ConfigurableListableBeanFactory.class });

		ReflectionUtils.makeAccessible(method);
		ReflectionUtils.invokeMethod(method, null, new Object[] { beanFactory });
	}

	/**
	 * Initialize the theme capability.
	 */
	protected void onRefresh() {
		super.onRefresh();
		this.themeSource = UiApplicationContextUtils.initThemeSource(this);
	}

	public Theme getTheme(String themeName) {
		return this.themeSource.getTheme(themeName);
	}

	/**
	 * The default location for the root context is
	 * "/WEB-INF/applicationContext.xml", and "/WEB-INF/test-servlet.xml" for a
	 * context with the namespace "test-servlet" (like for a DispatcherServlet
	 * instance with the servlet-name "test").
	 * 
	 * @see XmlWebApplicationContext#getDefaultConfigLocations()
	 * @see XmlWebApplicationContext#DEFAULT_CONFIG_LOCATION
	 */
	protected String[] getDefaultConfigLocations() {
		if (getNamespace() != null) {
			return new String[] { XmlWebApplicationContext.DEFAULT_CONFIG_LOCATION_PREFIX + getNamespace()
					+ XmlWebApplicationContext.DEFAULT_CONFIG_LOCATION_SUFFIX };
		}
		else {
			return new String[] { XmlWebApplicationContext.DEFAULT_CONFIG_LOCATION };
		}
	}

	/**
	 * Set the config locations for this application context in init-param
	 * style, i.e. with distinct locations separated by commas, semicolons or
	 * whitespace.
	 * <p>
	 * If not set, the implementation may use a default as appropriate.
	 * 
	 * @see ConfigurableApplicationContext#CONFIG_LOCATION_DELIMITERS
	 */
	public void setConfigLocation(String location) {
		setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
	}
}
