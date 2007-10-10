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
import org.springframework.core.io.Resource;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.Assert;

/**
 * Install Bundles using a FactoryBean.
 * 
 * This allows customers to use Spring to drive bundle management. Bundles
 * states can be modified using the state parameter. Most commonly this is set
 * to "start".
 * 
 * <p/>Pay attention when installing bundles dynamically since classes can be
 * loaded aggressively.
 * 
 * @author Andy Piper
 */
public class BundleFactoryBean implements FactoryBean, BundleContextAware, InitializingBean, DisposableBean {

	private static Log log = LogFactory.getLog(BundleFactoryBean.class);

	// bundle info

	/** Bundle location */
	protected Resource location;

	/** Bundle symName */
	protected String symbolicName;

	/** Actual bundle */
	private Bundle bundle;

	protected BundleContext bundleContext;

	private String action;

	private int startLevel;

	private ClassLoader classloader;

	/** unused at the moment */
	private boolean pushBundleAsContextClassLoader = false;

	/** wait until the operation invoked completes */
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

		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		try {
			if (pushBundleAsContextClassLoader) {
				Thread.currentThread().setContextClassLoader(classloader);
			}

			bundle = findBundle();

            Assert.notNull(symbolicName, "Bundle-SymbolicName is required");            
            Assert.notNull(bundle, "cannot find bundle with symbolic name=" + symbolicName);

			// if we get here, the bundle is installed already
			if (action == null || action.equalsIgnoreCase("install")) {
			}

			else if (action.equalsIgnoreCase("start")) {
				bundle.start();
			}
			else if (action.equalsIgnoreCase("update")) {
				if (location != null)
					bundle.update(location.getInputStream());
				else
					bundle.update();
			}
			else if (action.equalsIgnoreCase("stop")) {
				bundle.stop();
			}
			else if (action.equalsIgnoreCase("uninstall")) {
				bundle.uninstall();
			}
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

	/*
	 * No destruction behavior specified.
	 * 
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		bundle = null;
		classloader = null;
	}

	/**
	 * Find a bundle based on the configuration.
	 * 
	 * @return
	 * @throws Exception
	 */
	private Bundle findBundle() throws Exception {
		// check if the bundle has been installed, if we are able to
		Bundle bundle = null;

        if (symbolicName != null) {
            bundle = OsgiBundleUtils.findBundleBySymbolicName(bundleContext, symbolicName);
        }

		if (bundle == null) {
			Assert.notNull(location, "could not find an installed bundle with symbolicName=[" + symbolicName
					+ "] and no location was specified; cannot continue");

			log.info("Loading bundle [" + location.getURL());
			bundle = bundleContext.installBundle(location.getURL().toString());
            if (symbolicName == null) {
                symbolicName = bundle.getSymbolicName();
            }
        }

		// TODO: keep the start-level or not?
		// updateStartLevel(getStartLevel());

		return bundle;
	}

	public void setLocation(Resource url) throws Exception {
		location = url;
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

    public Resource getLocation() {
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

	public void setClassloader(ClassLoader classloader) {
		this.classloader = classloader;
	}

}
