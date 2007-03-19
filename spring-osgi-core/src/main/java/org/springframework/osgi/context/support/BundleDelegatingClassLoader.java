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

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
import org.springframework.aop.framework.Advised;
import org.springframework.osgi.util.OsgiBundleUtils;

/**
 * ClassLoader backed by an OSGi bundle. Will use the Bundle class loading.
 * Contains facilities for tracing classloading behavior so that issues can be
 * easily resolved. Debugging can be enabled by setting the system property
 * <code>org.springframework.osgi.DebugClassLoading</code> to true.
 *
 *
 * @author Adrian Colyer
 * @author Andy Piper
 * @author Costin Leau
 * @since 2.0
 */
public class BundleDelegatingClassLoader extends ClassLoader {
	private ClassLoader parent;

	private static final boolean DEBUG = Boolean.getBoolean("org.springframework.osgi.DebugClassLoading");
	private Bundle backingBundle;
	private static final Log log = LogFactory.getLog(BundleDelegatingClassLoader.class);

	public static BundleDelegatingClassLoader createBundleClassLoaderFor(final Bundle aBundle) {
		return (BundleDelegatingClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return new BundleDelegatingClassLoader(aBundle);
			}
		});
	}

	public static BundleDelegatingClassLoader createBundleClassLoaderFor(final Bundle aBundle, final ClassLoader parent) {
		return (BundleDelegatingClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return new BundleDelegatingClassLoader(aBundle, parent);
			}
		});
	}

	private BundleDelegatingClassLoader(Bundle aBundle) {
		this(aBundle, Advised.class.getClassLoader());
	}

	private BundleDelegatingClassLoader(Bundle aBundle, ClassLoader parentClassLoader) {
		super(parentClassLoader);
		this.backingBundle = aBundle;
		this.parent = parentClassLoader;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (!(o instanceof BundleDelegatingClassLoader))
			return false;

		final BundleDelegatingClassLoader bundleDelegatingClassLoader = (BundleDelegatingClassLoader) o;

		if (backingBundle.equals(bundleDelegatingClassLoader.backingBundle))
			return (parent == null || parent.equals(bundleDelegatingClassLoader.parent));

		return false;
	}

	public int hashCode() {
		int hashCode = backingBundle.hashCode();
		if (parent != null)
			hashCode |= parent.hashCode();

		return hashCode;
	}

	protected Class findClass(String name) throws ClassNotFoundException {
		try {
			try {
				return this.backingBundle.loadClass(name);
			}
			catch (ClassNotFoundException ex) {
				return parent.loadClass(name);
			}
		}
		catch (ClassNotFoundException cnfe) {
			if (log.isDebugEnabled() || DEBUG) {
				debugClassLoading(name, null);
			}
			throw cnfe;
		}
		catch (NoClassDefFoundError ncdfe) {
			// This is almost always an error
			if (log.isWarnEnabled() || DEBUG) {
				// This is caused by a dependent class failure,
				// so make sure we search for the right one.
				String cname = ncdfe.getMessage().replace('/', '.');
				debugClassLoading(cname, name);
			}
			throw ncdfe;
		}
	}

	/**
	 * A best-guess attempt at figuring out why the class could not be found.
	 *
	 * @param name of the class we are trying to find.
	 */
	private synchronized void debugClassLoading(String name, String root) {
		Dictionary dict = backingBundle.getHeaders();
		String bname = dict.get(Constants.BUNDLE_NAME) + "(" + dict.get(Constants.BUNDLE_SYMBOLICNAME) + ")";
		log.debug("Could not find class [" + name + "] required by [" + bname + "] scanning available bundles");

		BundleContext context = OsgiBundleUtils.getBundleContext(backingBundle);
		String packageName = name.substring(0, name.lastIndexOf('.'));
		// Reject global packages
		if (name.indexOf('.') < 0) {
			log.debug("Class is not in a package, its unlikely that this will work");
			return;
		}
		Version iversion = hasImport(backingBundle, packageName);
		if (iversion != null && context != null) {
			log.debug("Class is correctly imported as version [" + iversion + "], checking providing bundles");
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
									log.debug("Bundle [" + getBundleName(bundles[j]) + "] exports [" + root
											+ "] as version [" + rootexport
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
			log.debug("Class is exported, checking this bundle");
			checkBundleForClass(backingBundle, name, iversion);
		}
	}

	private Version checkBundleForClass(Bundle bundle, String name, Version iversion) {
		String packageName = name.substring(0, name.lastIndexOf('.'));
		Version hasExport = hasExport(bundle, packageName);

		// log.info("Examining Bundle [" + bundle.getBundleId() + ": " + bname +
		// "]");
		// Check for version matching
		if (hasExport != null && !hasExport.equals(iversion)) {
			log.debug("Bundle [" + getBundleName(bundle) + "] exports [" + packageName + "] as version [" + hasExport
					+ "] but version [" + iversion + "] was required");
			return hasExport;
		}
		// Do more detailed checks
		String cname = name.substring(packageName.length() + 1) + ".class";
		Enumeration e = bundle.findEntries("/" + packageName.replace('.', '/'), cname, false);
		if (e == null) {
			if (hasExport != null) {
				URL url = checkBundleJarsForClass(bundle, name);
				if (url != null) {
					log.debug("Bundle [" + getBundleName(bundle) + "] contains [" + cname + "] in embedded jar ["
							+ url.toString() + "] but exports the package");
				}
				else {
					log.debug("Bundle [" + getBundleName(bundle) + "] does not contain [" + cname
							+ "] but exports the package");
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
					log.debug("Bundle [" + getBundleName(bundle) + "] contains package [" + packageName
							+ "] and exports it");
				}
				else {
					log.debug("Bundle [" + getBundleName(bundle) + "] contains package [" + packageName
							+ "] but does not export it");
				}

			}
		}
		// Found the resource, check that it is exported.
		else {
			if (hasExport != null) {
				log.debug("Bundle [" + getBundleName(bundle) + "] contains resource [" + cname
						+ "] and it is correctly exported as version [" + hasExport + "]");
				Class c = null;
				try {
					c = bundle.loadClass(name);
				}
				catch (ClassNotFoundException e1) {
				}
				log.debug("Bundle [" + getBundleName(bundle) + "] loadClass [" + cname + "] returns [" + c + "]");
			}
			else {
				log.debug("Bundle [" + getBundleName(bundle) + "] contains resource [" + cname
						+ "] but its package is not exported");
			}
		}
		return hasExport;
	}

	private URL checkBundleJarsForClass(Bundle bundle, String name) {
		String cname = name.replace('.', '/') + ".class";
		for (Enumeration e = bundle.findEntries("/", "*.jar", true); e != null && e.hasMoreElements();) {
			URL url = (URL) e.nextElement();
			try {
				JarInputStream jin = new JarInputStream(url.openStream());
				// Copy entries from the real jar to our virtual jar
				for (JarEntry ze = jin.getNextJarEntry(); ze != null; ze = jin.getNextJarEntry()) {
					if (ze.getName().equals(cname)) {
						jin.close();
						return url;
					}
				}
				jin.close();
			}
			catch (IOException e1) {
				log.debug("Skipped " + url.toString() + ": " + e1.getMessage());
			}
		}
		return null;
	}

	private Version hasImport(Bundle bundle, String packageName) {
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

	private Version hasExport(Bundle bundle, String packageName) {
		Dictionary dict = bundle.getHeaders();
		return getVersion((String) dict.get(Constants.EXPORT_PACKAGE), packageName);
	}

	// Pull out a version of the meta-data
	private Version getVersion(String stmt, String packageName) {
		if (stmt != null) {
			for (StringTokenizer strok = new StringTokenizer(stmt, ","); strok.hasMoreTokens();) {
				StringTokenizer parts = new StringTokenizer(strok.nextToken(), ";");
				String pkg = parts.nextToken().trim();
				if (pkg.equals(packageName)) {
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

	private String getBundleName(Bundle bundle) {
		Dictionary dict = bundle.getHeaders();
		String name = (String) dict.get(Constants.BUNDLE_NAME);
		String sname = (String) dict.get(Constants.BUNDLE_SYMBOLICNAME);
		return (sname != null ? sname : name) + " (" + bundle.getLocation() + ")";
	}

	protected URL findResource(String name) {
		if (log.isDebugEnabled())
			log.debug("looking for resource " + name);
		URL url = this.backingBundle.getResource(name);

		if (url != null && log.isDebugEnabled())
			log.debug("found resource " + name + " at " + url);
		return url;
	}

	protected Enumeration findResources(String name) throws IOException {
		if (log.isDebugEnabled())
			log.debug("looking for resources " + name);

		Enumeration enm = this.backingBundle.getResources(name);

		if (enm != null && enm.hasMoreElements() && log.isDebugEnabled())
			log.debug("found resource " + name + " at " + this.backingBundle.getLocation());

		return enm;
	}

	public URL getResource(String name) {
		return (parent == null) ? findResource(name) : super.getResource(name);
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		return (parent == null) ? findClass(name) : super.loadClass(name);
	}

	// For testing
	public Bundle getBundle() {
		return backingBundle;
	}

}
