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

package org.springframework.osgi.test.platform;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.BeanUtils;
import org.springframework.osgi.test.internal.util.IOUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Knopflerfish 2.0.4+ Platform. Handles the 3.x line through reflection.
 * 
 * @author Costin Leau
 */
public class KnopflerfishPlatform extends AbstractOsgiPlatform {

	private static interface KFBootstrapper {
		BundleContext start();

		void stop();
	}

	private static class KF2Bootstrapper implements KFBootstrapper {
		private static final Class<?> BOOT_CLASS;
		private static final Constructor<?> CONSTRUCTOR;
		private static final Method LAUNCH;
		private static final Method GET_BUNDLE_CONTEXT;
		private static final Method SHUTDOWN;

		static {
			BOOT_CLASS = ClassUtils.resolveClassName(KF_2X_BOOT_CLASS, KF2Bootstrapper.class.getClassLoader());

			try {
				CONSTRUCTOR = BOOT_CLASS.getDeclaredConstructor(Object.class);
			} catch (NoSuchMethodException nsme) {
				throw new IllegalArgumentException("Invalid framework class", nsme);
			}

			LAUNCH = BeanUtils.findDeclaredMethod(BOOT_CLASS, "launch", new Class[] { long.class });
			GET_BUNDLE_CONTEXT =
					org.springframework.util.ReflectionUtils.findMethod(BOOT_CLASS, "getSystemBundleContext");
			SHUTDOWN = org.springframework.util.ReflectionUtils.findMethod(BOOT_CLASS, "shutdown");
		}

		private final Object monitor;
		private Object framework;

		KF2Bootstrapper(Object monitor) {
			this.monitor = monitor;

		}

		public BundleContext start() {
			framework = BeanUtils.instantiateClass(CONSTRUCTOR, monitor);
			org.springframework.util.ReflectionUtils.invokeMethod(LAUNCH, framework, Integer.valueOf(0));
			return (BundleContext) org.springframework.util.ReflectionUtils.invokeMethod(GET_BUNDLE_CONTEXT, framework);
		}

		public void stop() {
			if (framework != null) {
				org.springframework.util.ReflectionUtils.invokeMethod(SHUTDOWN, framework);
				framework = null;
			}
		}
	}

	private static class KF3Bootstrapper implements KFBootstrapper {
		private static final Class<?> BOOT_CLASS;
		private static final Constructor<?> CONSTRUCTOR;
		private static final Method INIT;
		private static final Method LAUNCH;
		private static final Method GET_BUNDLE_CONTEXT;
		private static final Method SHUTDOWN;

		private Object framework;
		private Map properties;

		static {
			BOOT_CLASS = ClassUtils.resolveClassName(KF_3X_BOOT_CLASS, KF3Bootstrapper.class.getClassLoader());

			try {
				CONSTRUCTOR = BOOT_CLASS.getDeclaredConstructor(Map.class, BOOT_CLASS);
			} catch (NoSuchMethodException nsme) {
				throw new IllegalArgumentException("Invalid framework class", nsme);
			}

			INIT = BeanUtils.findDeclaredMethod(BOOT_CLASS, "init", null);
			ReflectionUtils.makeAccessible(INIT);
			LAUNCH = BeanUtils.findDeclaredMethod(BOOT_CLASS, "launch", null);
			GET_BUNDLE_CONTEXT =
					org.springframework.util.ReflectionUtils.findMethod(BOOT_CLASS, "getSystemBundleContext");
			SHUTDOWN = org.springframework.util.ReflectionUtils.findMethod(BOOT_CLASS, "shutdown");
		}

		KF3Bootstrapper(Map properties) {
			this.properties = properties;
		}

		public BundleContext start() {
			// Main main = new Main();
			// Method mt = ReflectionUtils.findMethod(Main.class, "handleArgs", String[].class);
			// ReflectionUtils.makeAccessible(mt);
			// ReflectionUtils.invokeMethod(mt, main, new Object[] { new String[] { "-launch", "-init" } });
			// Field fl = ReflectionUtils.findField(main.getClass(), "framework");
			// ReflectionUtils.makeAccessible(fl);
			// return OsgiBundleUtils.getBundleContext((Bundle) ReflectionUtils.getField(fl, main));

			framework = BeanUtils.instantiateClass(CONSTRUCTOR, properties, null);
			
			Field systemBundle = ReflectionUtils.findField(framework.getClass(), "systemBundle");
			ReflectionUtils.makeAccessible(systemBundle);

			Bundle bundle = (Bundle) ReflectionUtils.getField(systemBundle, framework);
			Method mt = ReflectionUtils.findMethod(bundle.getClass(), "init");
			ReflectionUtils.invokeMethod(mt, bundle);

			
			ReflectionUtils.invokeMethod(INIT, framework);
			ReflectionUtils.invokeMethod(LAUNCH, framework);

			try {
				bundle.start();
			} catch (Exception ex) {
				throw new IllegalStateException("Cannot start framework", ex);
			}

			return (BundleContext) org.springframework.util.ReflectionUtils.invokeMethod(GET_BUNDLE_CONTEXT, framework);
		}

		public void stop() {
			if (framework != null) {
				org.springframework.util.ReflectionUtils.invokeMethod(SHUTDOWN, framework);
				framework = null;
			}
		}
	}

	private static final String KF_2X_BOOT_CLASS = "org.knopflerfish.framework.Framework";
	private static final String KF_3X_BOOT_CLASS = "org.knopflerfish.framework.FrameworkContext";
	private static final boolean KF_2X =
			ClassUtils.isPresent(KF_2X_BOOT_CLASS, KnopflerfishPlatform.class.getClassLoader());

	private BundleContext context;
	private KFBootstrapper framework;
	private File kfStorageDir;

	public KnopflerfishPlatform() {
		toString = "Knopflerfish OSGi Platform";
	}

	Properties getPlatformProperties() {
		if (kfStorageDir == null) {
			kfStorageDir = createTempDir("kf");
			kfStorageDir.deleteOnExit();
			if (log.isDebugEnabled())
				log.debug("KF temporary storage dir is " + kfStorageDir.getAbsolutePath());

		}

		// default properties
		Properties props = new Properties();
		props.setProperty("org.osgi.framework.dir", kfStorageDir.getAbsolutePath());
		props.setProperty("org.knopflerfish.framework.bundlestorage", "file");
		props.setProperty("org.knopflerfish.framework.bundlestorage.file.reference", "true");
		props.setProperty("org.knopflerfish.framework.bundlestorage.file.unpack", "false");
		props.setProperty("org.knopflerfish.startlevel.use", "true");
		props.setProperty("org.knopflerfish.osgi.setcontextclassloader", "true");
		// embedded mode
		props.setProperty("org.knopflerfish.framework.exitonshutdown", "false");
		// disable patch CL
		props.setProperty("org.knopflerfish.framework.patch", "false");
		// new in KF 2.0.4 - automatically exports system packages based on the JRE version
		props.setProperty("org.knopflerfish.framework.system.export.all", "true");

		return props;
	}

	public BundleContext getBundleContext() {
		return context;
	}

	public void start() throws Exception {
		if (framework == null) {
			// copy configuration properties to sys properties
			System.getProperties().putAll(getConfigurationProperties());
			framework = (KF_2X ? new KF2Bootstrapper(this) : new KF3Bootstrapper(getPlatformProperties()));
			context = framework.start();
		}
	}

	public void stop() throws Exception {
		if (framework != null) {
			context = null;
			try {
				framework.stop();
			} finally {
				framework = null;
				IOUtils.delete(kfStorageDir);
			}
		}
	}
}