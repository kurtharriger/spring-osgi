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

import org.apache.commons.logging.Log;
import org.knopflerfish.framework.FrameworkFactoryImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;
import org.springframework.beans.BeanUtils;
import org.springframework.osgi.test.internal.util.IOUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Knopflerfish 2.0.4+/3.x Platform. Automatically detects the available version on the class path and uses the
 * appropriate means to configure and instantiate it.
 * 
 * @author Costin Leau
 */
public class KnopflerfishPlatform extends AbstractOsgiPlatform {

	private static class KF2Platform implements Platform {
		private static final Class<?> BOOT_CLASS;
		private static final Constructor<?> CONSTRUCTOR;
		private static final Method LAUNCH;
		private static final Method GET_BUNDLE_CONTEXT;
		private static final Method SHUTDOWN;

		static {
			BOOT_CLASS = ClassUtils.resolveClassName(KF_2X_BOOT_CLASS, KF2Platform.class.getClassLoader());

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

		KF2Platform(Object monitor) {
			this.monitor = monitor;

		}

		public BundleContext start() {
			framework = BeanUtils.instantiateClass(CONSTRUCTOR, monitor);
			ReflectionUtils.invokeMethod(LAUNCH, framework, Integer.valueOf(0));
			return (BundleContext) ReflectionUtils.invokeMethod(GET_BUNDLE_CONTEXT, framework);
		}

		public void stop() {
			if (framework != null) {
				ReflectionUtils.invokeMethod(SHUTDOWN, framework);
				framework = null;
			}
		}
	}

	private static class KF3Platform implements Platform {
		private Bundle framework;
		private final Map properties;
		private final Log log;
		private FrameworkTemplate fwkTemplate;

		KF3Platform(Map properties, Log log) {
			this.properties = properties;
			this.log = log;
		}

		public BundleContext start() {
			framework = new FrameworkFactoryImpl().newFramework(properties);
			fwkTemplate = new DefaultFrameworkTemplate(framework, log);
			fwkTemplate.init();
			fwkTemplate.start();

			return framework.getBundleContext();
		}

		public void stop() {
			if (fwkTemplate != null) {
				fwkTemplate.stopAndWait(1000);
				fwkTemplate = null;
			}
		}
	}

	private static final String KF_2X_BOOT_CLASS = "org.knopflerfish.framework.Framework";
	private static final String KF_3X_BOOT_CLASS = "org.knopflerfish.framework.FrameworkContext";
	private static final boolean KF_2X =
			ClassUtils.isPresent(KF_2X_BOOT_CLASS, KnopflerfishPlatform.class.getClassLoader());

	private BundleContext context;
	private Platform framework;
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
		// props.setProperty("org.knopflerfish.framework.system.export.all_15", "true");
		// add strict bootpath delegation (introduced in KF 2.3.0)
		// since otherwise classes will be loaded from the booth classpath
		// when generating JDK proxies instead of the OSGi space
		// since KF thinks that a non-OSGi class is making the call.
		props.setProperty("org.knopflerfish.framework.strictbootclassloading", "true");

		return props;
	}

	public BundleContext getBundleContext() {
		return context;
	}

	public void start() throws Exception {
		if (framework == null) {
			// copy configuration properties to sys properties
			System.getProperties().putAll(getConfigurationProperties());
			framework = (KF_2X ? new KF2Platform(this) : new KF3Platform(getPlatformProperties(), log));
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