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

package org.springframework.osgi.web.extender.internal.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.osgi.service.importer.support.ImportContextClassLoader;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class for IO operations regarding web integration.
 * 
 * @author Costin Leau
 */
public abstract class Utils {

	/** logger */
	private static final Log log = LogFactory.getLog(Utils.class);

	private static final String SLASH = "/";

	/** Jasper class */
	// org.apache.jasper.JspC
	private static final String JASPER_CLASS = "org.apache.jasper.servlet.JspServlet";


	// might have to improve this method to cope with missing folder entries ...

	/**
	 * Copies the given bundle content to the given target folder. This means
	 * unpacking the bundle archive. In case of a failure, an exception is
	 * thrown.
	 * 
	 * @param bundle
	 * @param targetFolder
	 */
	public static void unpackBundle(Bundle bundle, File targetFolder) {
		// no need to use a recursive method since we get all resources directly
		Enumeration enm = bundle.findEntries("/", null, true);
		while (enm != null && enm.hasMoreElements()) {
			boolean trace = log.isTraceEnabled();

			// get only the path
			URL url = (URL) enm.nextElement();
			String entryPath = url.getPath();
			if (entryPath.startsWith("/"))
				entryPath = entryPath.substring(1);

			File targetFile = new File(targetFolder, entryPath);
			// folder are a special case, we have to create them rather then copy
			if (entryPath.endsWith("/"))
				targetFile.mkdir();
			else {
				try {
					OutputStream targetStream = new FileOutputStream(targetFile);
					if (trace)
						log.trace("copying " + url + " to " + targetFile);
					FileCopyUtils.copy(url.openStream(), targetStream);
				}
				catch (IOException ex) {
					//
					log.error("cannot copy resource " + entryPath, ex);
					throw (RuntimeException) new IllegalStateException("IO exception while unpacking bundle "
							+ OsgiStringUtils.nullSafeNameAndSymName(bundle)).initCause(ex);
				}
				// no need to close the streams - the utils already handles that
			}
		}
	}

	/**
	 * Determines the context path assuming the given bundle is a war.. Will try
	 * to return a string representation by first looking at the bundle
	 * location, followed by the bundle symbolic name and then name. If none can
	 * be used (for non-OSGi artifacts for example) then an arbitrary name will
	 * be used.
	 * 
	 * 
	 * @param bundle
	 * @return
	 */
	public static String getWarContextPath(Bundle bundle) {
		// get only the file (be sure to normalize just in case)
		String path = StringUtils.getFilename(StringUtils.cleanPath(bundle.getLocation()));
		// remove extension
		path = StringUtils.stripFilenameExtension(path);

		if (!StringUtils.hasText(path)) {
			// fall-back to bundle symbolic name
			path = bundle.getSymbolicName();
			if (!StringUtils.hasText(path)) {
				// fall-back to bundle name
				path = OsgiStringUtils.nullSafeName(bundle);
			}
		}
		return (path.startsWith(SLASH) ? path : SLASH.concat(path));
	}

	/**
	 * Returns the defining classloader of the given class. As we're running
	 * inside an OSGi classloader, the classloaders that are able to load the
	 * resource, are not always the defining classloader.
	 * 
	 * @param className
	 * @param ClassLoader
	 * @return
	 */
	public static ClassLoader getClassLoader(String className, ClassLoader classLoader) {
		try {
			Class clazz = ClassUtils.forName(className, classLoader);
			return clazz.getClassLoader();
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Dedicated utility method used for creating an OSGi reference to
	 * Jetty/Tomcat/XXX server service.
	 * 
	 * @return proxy to the found OSGi service
	 */
	public static Object createServerServiceProxy(BundleContext bundleContext, Class proxyType, String serviceName) {

		OsgiServiceProxyFactoryBean proxyFB = new OsgiServiceProxyFactoryBean();

		// create a bridged classloader so that all the proxy dependencies are considered

		// first between the extender bundle and spring-aop (so that the proxy infrastructure classes are seen)
		// TODO: OSGI-350
		BundleDelegatingClassLoader cl = BundleDelegatingClassLoader.createBundleClassLoaderFor(
			bundleContext.getBundle(), DefaultAopProxyFactory.class.getClassLoader());

		proxyFB.setBeanClassLoader(cl);
		proxyFB.setBundleContext(bundleContext);
		proxyFB.setContextClassLoader(ImportContextClassLoader.UNMANAGED);
		proxyFB.setInterfaces(new Class[] { proxyType });
		if (StringUtils.hasText(serviceName))
			proxyFB.setServiceBeanName(serviceName);
		proxyFB.afterPropertiesSet();

		return proxyFB.getObject();
	}

	/**
	 * Detects the Jasper/JSP parser (used by the server) and returns a chained
	 * class-loader which incorporates them all. This allows the web application
	 * to use servlets and JSP w/o importing them (just like in traditional
	 * environments).
	 * 
	 * @return chained classloader containing javax. packages and the sever
	 * classes + Jasper/JSP compiler (if present)
	 */
	public static ClassLoader chainedWebClassLoaders(Class serverClass) {
		Assert.notNull(serverClass);
		ClassLoader serverLoader = serverClass.getClassLoader();
		ClassLoader jasperLoader = getClassLoader(JASPER_CLASS, serverLoader);

		if (serverLoader == jasperLoader)
			return serverLoader;
		else {
			// use the extender classloader
			jasperLoader = getClassLoader(JASPER_CLASS, Utils.class.getClassLoader());
			if (jasperLoader == null)
				return serverLoader;
			else {
				return new ChainedClassLoader(new ClassLoader[] { serverLoader, jasperLoader });
			}
		}
	}
}
