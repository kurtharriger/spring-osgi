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

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.startlevel.StartLevel;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.core.io.Resource;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.Assert;

/**
 * Install Bundles using a FactoryBean. This allows customers to use Spring to
 * drive bundle management. Bundles states can be modified using the state parameter.
 * Most commonly this is set to "start".
 * <p/>
 * Note that there are some issues with installing bundles dynamically in that Spring
 * aggressively loads bean classes so its not currently possible to have bean definitions that
 * use the same bundle in the same application context file.
 *
 * @author Andy Piper
 */
public class BundleFactoryBean implements FactoryBean, BundleContextAware,
	SynchronousBundleListener, InitializingBean, DisposableBean {
	private Resource bundleUrl;
	private Bundle bundle;
	private BundleContext bundleContext;
	private String state;
	private int startLevel;
	private ClassLoader classloader;
	private String symbolicName;
	private static Log log = LogFactory.getLog(BundleFactoryBean.class);
	private boolean pushBundleAsContextClassLoader = false;
	private final Latch latch = new Latch();

	public BundleFactoryBean() {
	}

	public Object getObject() throws Exception {
		return getBundle();
	}

	public synchronized Bundle getBundle() throws Exception {
		Assert.notNull(bundleUrl, "location is required");
		Assert.notNull(bundleContext, "BundleContext is required");

		if (bundle == null) {
			if (log.isInfoEnabled()) {
				log.info("Loading bundle [" + bundleUrl.getURL());
			}
			bundle = bundleContext.installBundle(bundleUrl.getURL().toString());
			classloader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle);
			bundleContext.addBundleListener(this);

		}
		// Set the start level
		updateStartLevel(getStartLevel());
		return bundle;
	}

	public Class getObjectType() {
		return Bundle.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void setLocation(Resource url) throws Exception {
		bundleUrl = url;
	}

	public Resource getLocation() {
		return bundleUrl;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	public void setBundleContext(BundleContext context) {
		// System.err.println("setBundleContext(" + context + ")");
		bundleContext = context;
	}

	public BundleContext getBundleContext() {
		return bundleContext;
	}

	public synchronized void setState(String level) {
		state = level;
	}

	public synchronized String getState() {
		return state;
	}

	public int getStartLevel() {
		return startLevel;
	}

	public void setStartLevel(int startLevel) {
		this.startLevel = startLevel;
	}

	/**
	 * Determines whether invocations on the remote service should be performed in the context
	 * of the target bundle's ClassLoader. The default is false.
	 */
	public boolean isPushBundleAsContextClassloader() {
		return pushBundleAsContextClassLoader;
	}

	/**
	 * Determines whether invocations on the remote service should be performed in the context
	 * of the target bundle's ClassLoader. The default is false.
	 *
	 * @param pushBundleAsContextClassLoader
	 */
	public void setPushBundleAsContextClassloader(boolean pushBundleAsContextClassLoader) {
		this.pushBundleAsContextClassLoader = pushBundleAsContextClassLoader;
	}

	public synchronized void afterPropertiesSet() throws Exception {
		// REVIEW andyp -- we optionally push the Bundle's classloader as the CCL before
		// invoking any lifecycle tasks.
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		try {
			if (pushBundleAsContextClassLoader) {
				Thread.currentThread().setContextClassLoader(classloader);
			}

			Bundle b = getBundle();
			if (state == null || state.equalsIgnoreCase("install")) {
				// default is to install the bundle.
			}
			else if (state.equalsIgnoreCase("start")) {
				// Don't try and start if we are not resolved.
				// FIXME andyp -- this doesn't work as expected.
				// if (b.getState() == Bundle.RESOLVED) {
				b.start();
				waitForContextCreation(b);
				// }
			}
			else if (state.equalsIgnoreCase("stop")) {
				b.stop();
			}
			else if (state.equalsIgnoreCase("uninstall")) {
				b.uninstall();
			}
			else if (state.equalsIgnoreCase("update")) {
				if ((b.getState() & (Bundle.RESOLVED | Bundle.ACTIVE)) != 0) {
					b.update();
				}
			}
		}
		catch (BundleException e) {
			// FIXME andyp -- convert to something spring-like
			log.error(e.getMessage(), e);
			throw e;
		}
		finally {
			if (pushBundleAsContextClassLoader) {
				Thread.currentThread().setContextClassLoader(ccl);
			}
		}
	}

	private void waitForContextCreation(final Bundle b) throws Exception {
		ApplicationContextConfiguration config = new ApplicationContextConfiguration(b);
		// Wait for the Spring artifacts to be created
		// We could make this configurable.
		if (config.isSpringPoweredBundle()) {
			ServiceReference sref = bundleContext.getServiceReference(ApplicationEventMulticaster.class.getName());
			ApplicationEventMulticaster ctx = (ApplicationEventMulticaster) bundleContext.getService(sref);
			Assert.notNull(ctx);
			ctx.addApplicationListener(new ApplicationListener() {
				public void onApplicationEvent(ApplicationEvent event) {
					SpringBundleEvent ev = (SpringBundleEvent) event;
					if (((Bundle) ev.getSource()).getBundleId() == b.getBundleId()) {
						if (ev.getType() == BundleEvent.STARTED
							|| ev.getType() == BundleEvent.STOPPED) {
							latch.decr();
						}
					}
				}
			});
			latch.await();
		}
	}


	private void updateStartLevel(int level) {
		if (level == 0 || bundle == null) return;
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

	public synchronized void bundleChanged(BundleEvent event) {
		if (bundle == null) return;
		// System.out.println("EVENT: " + event.getBundle().getSymbolicName() + " " + event.getType());
		if (event.getBundle().getBundleId() == bundle.getBundleId()) {
			switch (event.getType()) {
				case BundleEvent.UNINSTALLED:
					latch.reset();
					bundle = null;
					classloader = null;
					bundleContext.removeBundleListener(this);
					break;
					/*
									case BundleEvent.RESOLVED:
										if (log.isInfoEnabled()) log.info("Bundle [" + beanName
												+ "] was resolved, updating properties");
										try {
											afterPropertiesSet();
										}
										catch (Exception e) {
											if (log.isWarnEnabled()) log.warn("Could not change bundle state", e);
										}
										break;
									*/
				default:
					break;
			}
		}
	}

	public void destroy() throws Exception {
		if (bundle != null) {
			// System.out.println("destroy(" + bundle + ")");
			bundle.uninstall();
			latch.reset();
			bundle = null;
			classloader = null;
			bundleContext.removeBundleListener(this);
		}
	}

	public ClassLoader getClassloader() {
		return classloader;
	}

	public void setClassloader(ClassLoader classloader) {
		this.classloader = classloader;
	}

	private class Latch {
		private int count = 1;

		synchronized void await() throws Exception {
			while (count > 0) {
				wait();
			}
		}

		synchronized void decr() {
			count--;
			notifyAll();
		}

		synchronized void reset() {
			count = 1;
		}
	}

	/*
	public static void dumpStacks() {
		Map stacks = Thread.getAllStackTraces();
		for (Iterator i = stacks.entrySet().iterator(); i.hasNext();) {
			Map.Entry e = (Map.Entry) i.next();
			System.out.println("");
			System.out.println(e.getKey());
			StackTraceElement[] se = (StackTraceElement[]) e.getValue();
			for (int j = 0; j < se.length; j++) {
				System.out.println(" " + se[j]);
			}
		}
	}
	*/
}
