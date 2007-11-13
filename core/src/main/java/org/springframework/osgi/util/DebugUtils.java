/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.util;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.springframework.util.ClassUtils;

/**
 * Utility class used for debugging exceptions in OSGi environment, such as
 * classloading errors.
 * 
 * The main entry point is
 * {@link #debugNoClassDefFoundWhenProxying(NoClassDefFoundError, BundleContext, Class[])}
 * which will try to determine the cause by trying to load the given interfaces
 * using the given bundle.
 * 
 * The search can be potentially expensive.
 * 
 * @author Costin Leau
 * @author Andy Piper
 */
// FIXME: clarify class usage contract
public abstract class DebugUtils {

	private static final Log log = LogFactory.getLog(DebugUtils.class);

	/**
	 * Debug NCDFE that occurs when creating proxies. Looks at what classes are
	 * visible by signature classes classloaders, logging a summary on debug
	 * level and the entire discovery process on trace level.
	 * 
	 * @param ncdfe NoClassDefFoundException cause
	 * @param bundleContext running bundle context
	 * @param interfaces ???
	 */
	public static String debugNoClassDefFoundWhenProxying(NoClassDefFoundError ncdfe, BundleContext bundleContext,
			Class[] interfaces) {

		String cname = ncdfe.getMessage().replace('/', '.');
		debugClassLoading(bundleContext.getBundle(), cname, null);

		StringBuffer message = new StringBuffer();
		// Check out all the classes.
		for (int i = 0; i < interfaces.length; i++) {
			ClassLoader cl = interfaces[i].getClassLoader();
			String cansee = "cannot";
			if (ClassUtils.isPresent(cname, cl))
				cansee = "can";
			message.append(interfaces[i] + " is loaded by " + cl + " which " + cansee + " see " + cname);
		}
		log.debug(message);
		return message.toString();
	}

	/**
	 * A best-guess attempt at figuring out why the class could not be found.
	 * 
	 * @param backingBundle ???
	 * @param name of the class we are trying to find.
	 * @param root ???
	 */
	public static void debugClassLoading(Bundle backingBundle, String name, String root) {
		boolean trace = log.isTraceEnabled();
		if (!trace)
			return;

		Dictionary dict = backingBundle.getHeaders();
		String bname = dict.get(Constants.BUNDLE_NAME) + "(" + dict.get(Constants.BUNDLE_SYMBOLICNAME) + ")";
		if (trace)
			log.trace("Could not find class [" + name + "] required by [" + bname + "] scanning available bundles");

		BundleContext context = OsgiBundleUtils.getBundleContext(backingBundle);
		String packageName = name.substring(0, name.lastIndexOf('.'));
		// Reject global packages
		if (name.indexOf('.') < 0) {
			if (trace)
				log.trace("Class is not in a package, its unlikely that this will work");
			return;
		}
		Version iversion = hasImport(backingBundle, packageName);
		if (iversion != null && context != null) {
			if (trace)
				log.trace("Class is correctly imported as version [" + iversion + "], checking providing bundles");
			Bundle[] bundles = context.getBundles();
			for (int i = 0; i < bundles.length; i++) {
				if (bundles[i].getBundleId() != backingBundle.getBundleId()) {
					Version exported = checkBundleForClass(bundles[i], name, iversion);
					// Everything looks ok, but is the root bundle importing the
					// dependent class also?
					if (exported != null && exported.equals(iversion) && root != null) {
						for (int j = 0; j < bundles.length; j++) {
							Version rootexport = hasExport(bundles[j], root.substring(0, root.lastIndexOf('.')));
							if (rootexport != null) {
								// TODO -- this is very rough, check the bundle
								// classpath also.
								Version rootimport = hasImport(bundles[j], packageName);
								if (rootimport == null || !rootimport.equals(iversion)) {
									if (trace)
										log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundles[j])
												+ "] exports [" + root + "] as version [" + rootexport
												+ "] but does not import dependent package [" + packageName
												+ "] at version [" + iversion + "]");
								}
							}
						}
					}
				}
			}
		}
		if (hasExport(backingBundle, packageName) != null) {
			if (trace)
				log.trace("Class is exported, checking this bundle");
			checkBundleForClass(backingBundle, name, iversion);
		}
	}

	private static Version checkBundleForClass(Bundle bundle, String name, Version iversion) {
		String packageName = name.substring(0, name.lastIndexOf('.'));
		Version hasExport = hasExport(bundle, packageName);

		// log.info("Examining Bundle [" + bundle.getBundleId() + ": " + bname +
		// "]");
		// Check for version matching
		if (hasExport != null && !hasExport.equals(iversion)) {
			log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] exports [" + packageName
					+ "] as version [" + hasExport + "] but version [" + iversion + "] was required");
			return hasExport;
		}
		// Do more detailed checks
		String cname = name.substring(packageName.length() + 1) + ".class";
		Enumeration e = bundle.findEntries("/" + packageName.replace('.', '/'), cname, false);
		if (e == null) {
			if (hasExport != null) {
				URL url = checkBundleJarsForClass(bundle, name);
				if (url != null) {
					log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] contains [" + cname
							+ "] in embedded jar [" + url.toString() + "] but exports the package");
				}
				else {
					log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] does not contain ["
							+ cname + "] but exports the package");
				}
			}

			String root = "/";
			String fileName = packageName;
			if (packageName.lastIndexOf(".") >= 0) {
				root = root + packageName.substring(0, packageName.lastIndexOf(".")).replace('.', '/');
				fileName = packageName.substring(packageName.lastIndexOf(".") + 1).replace('.', '/');
			}
			Enumeration pe = bundle.findEntries(root, fileName, false);
			if (pe != null) {
				if (hasExport != null) {
					log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] contains package ["
							+ packageName + "] and exports it");
				}
				else {
					log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] contains package ["
							+ packageName + "] but does not export it");
				}

			}
		}
		// Found the resource, check that it is exported.
		else {
			if (hasExport != null) {
				log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] contains resource [" + cname
						+ "] and it is correctly exported as version [" + hasExport + "]");
				Class c = null;
				try {
					c = bundle.loadClass(name);
				}
				catch (ClassNotFoundException e1) {
					// Ignored
				}
				log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] loadClass [" + cname
						+ "] returns [" + c + "]");
			}
			else {
				log.trace("Bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] contains resource [" + cname
						+ "] but its package is not exported");
			}
		}
		return hasExport;
	}

	private static URL checkBundleJarsForClass(Bundle bundle, String name) {
		String cname = name.replace('.', '/') + ".class";
		for (Enumeration e = bundle.findEntries("/", "*.jar", true); e != null && e.hasMoreElements();) {
			URL url = (URL) e.nextElement();
			JarInputStream jin = null;
			try {
				jin = new JarInputStream(url.openStream());
				// Copy entries from the real jar to our virtual jar
				for (JarEntry ze = jin.getNextJarEntry(); ze != null; ze = jin.getNextJarEntry()) {
					if (ze.getName().equals(cname)) {
						jin.close();
						return url;
					}
				}
			}
			catch (IOException e1) {
				log.trace("Skipped " + url.toString() + ": " + e1.getMessage());
			}

			finally {
				if (jin != null) {
					try {
						jin.close();
					}
					catch (Exception ex) {
						// don't do a thing
					}
				}
			}

		}
		return null;
	}

	/**
	 * Get the version of a package import from a bundle.
	 * 
	 * @param bundle
	 * @param packageName
	 * @return
	 */
	private static Version hasImport(Bundle bundle, String packageName) {
		Dictionary dict = bundle.getHeaders();
		// Check imports
		String imports = (String) dict.get(Constants.IMPORT_PACKAGE);
		Version v = getVersion(imports, packageName);
		if (v != null) {
			return v;
		}
		// Check for dynamic imports
		String dynimports = (String) dict.get(Constants.DYNAMICIMPORT_PACKAGE);
		if (dynimports != null) {
			for (StringTokenizer strok = new StringTokenizer(dynimports, ","); strok.hasMoreTokens();) {
				StringTokenizer parts = new StringTokenizer(strok.nextToken(), ";");
				String pkg = parts.nextToken().trim();
				if (pkg.endsWith(".*") && packageName.startsWith(pkg.substring(0, pkg.length() - 2)) || pkg.equals("*")) {
					Version version = Version.emptyVersion;
					for (; parts.hasMoreTokens();) {
						String modifier = parts.nextToken().trim();
						if (modifier.startsWith("version")) {
							version = Version.parseVersion(modifier.substring(modifier.indexOf("=") + 1).trim());
						}
					}
					return version;
				}
			}
		}
		return null;
	}

	private static Version hasExport(Bundle bundle, String packageName) {
		Dictionary dict = bundle.getHeaders();
		return getVersion((String) dict.get(Constants.EXPORT_PACKAGE), packageName);
	}

	/**
	 * Get the version of a package name.
	 * 
	 * @param stmt
	 * @param packageName
	 * @return
	 */
	private static Version getVersion(String stmt, String packageName) {
		if (stmt != null) {
			for (StringTokenizer strok = new StringTokenizer(stmt, ","); strok.hasMoreTokens();) {
				StringTokenizer parts = new StringTokenizer(strok.nextToken(), ";");
				String pkg = parts.nextToken().trim();
				if (pkg.equals(packageName)) {
					Version version = Version.emptyVersion;
					for (; parts.hasMoreTokens();) {
						String modifier = parts.nextToken().trim();
						if (modifier.startsWith("version")) {
							String vstr = modifier.substring(modifier.indexOf("=") + 1).trim();
							if (vstr.startsWith("\""))
								vstr = vstr.substring(1);
							if (vstr.endsWith("\""))
								vstr = vstr.substring(0, vstr.length() - 1);
							version = Version.parseVersion(vstr);
						}
					}
					return version;
				}
			}
		}
		return null;
	}
}
