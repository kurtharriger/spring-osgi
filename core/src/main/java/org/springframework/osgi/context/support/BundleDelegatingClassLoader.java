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
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiPlatformDetector;

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
	private ClassLoader bridge;
	private Bundle backingBundle;

    private static final Log log = LogFactory.getLog(BundleDelegatingClassLoader.class);

    public static BundleDelegatingClassLoader createBundleClassLoaderFor(Bundle aBundle) {
		return createBundleClassLoaderFor(aBundle, ProxyFactory.class.getClassLoader());
	}

	public static BundleDelegatingClassLoader createBundleClassLoaderFor(final Bundle bundle,
                                                                         final ClassLoader bridge) {
		return (BundleDelegatingClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return new BundleDelegatingClassLoader(bundle, bridge);
			}
		});
	}

	private BundleDelegatingClassLoader(Bundle bundle, ClassLoader bridgeLoader) {
		super(null);
		this.backingBundle = bundle;
		this.bridge = bridgeLoader;
    }

	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (!(o instanceof BundleDelegatingClassLoader))
			return false;

		final BundleDelegatingClassLoader bundleDelegatingClassLoader = (BundleDelegatingClassLoader) o;

		if (backingBundle.equals(bundleDelegatingClassLoader.backingBundle))
			return (bridge == null || bridge.equals(bundleDelegatingClassLoader.bridge));

		return false;
	}

	public int hashCode() {
		int hashCode = backingBundle.hashCode();
		if (bridge != null)
			hashCode |= bridge.hashCode();

		return hashCode;
	}

	protected Class findClass(String name) throws ClassNotFoundException {
		try {
			return this.backingBundle.loadClass(name);
		}
		catch (ClassNotFoundException cnfe) {
			if (log.isTraceEnabled()) {
				debugClassLoading(name, null);
			}
			throw new ClassNotFoundException(name +
                                             " not found from bundle [" +
                                             backingBundle.getSymbolicName() +
                                             "]", cnfe);
		}
		catch (NoClassDefFoundError ncdfe) {
			// This is almost always an error
			if (log.isTraceEnabled()) {
				// This is caused by a dependent class failure,
				// so make sure we search for the right one.
				String cname = ncdfe.getMessage().replace('/', '.');
				debugClassLoading(cname, name);
            }
            NoClassDefFoundError e = new NoClassDefFoundError(name +
                                                              " not found from bundle [" +
                                                              backingBundle.getSymbolicName() +
                                                              "]");
            e.initCause(ncdfe);
			throw e;
		}
	}


	private synchronized void debugClassLoading(String name, String root) {
		debugClassLoading(backingBundle, name, root);
	}

	/**
	 * A best-guess attempt at figuring out why the class could not be found.
	 *
	 * @param name of the class we are trying to find.
	 */
	public static void debugClassLoading(Bundle backingBundle, String name, String root) {
		Dictionary dict = backingBundle.getHeaders();
		String bname = dict.get(Constants.BUNDLE_NAME) + "(" + dict.get(Constants.BUNDLE_SYMBOLICNAME) + ")";
		if (log.isTraceEnabled())
			log.trace("Could not find class [" + name + "] required by [" + bname + "] scanning available bundles");

		BundleContext context = OsgiBundleUtils.getBundleContext(backingBundle);
		String packageName = name.substring(0, name.lastIndexOf('.'));
		// Reject global packages
		if (name.indexOf('.') < 0) {
			if (log.isTraceEnabled())
				log.trace("Class is not in a package, its unlikely that this will work");
			return;
		}
		Version iversion = hasImport(backingBundle, packageName);
		if (iversion != null && context != null) {
			if (log.isTraceEnabled())
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
									if (log.isTraceEnabled())
										log.trace("Bundle [" + getBundleName(bundles[j]) + "] exports [" + root
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
			if (log.isTraceEnabled())
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
			if (log.isTraceEnabled())
				log.trace("Bundle [" + getBundleName(bundle) + "] exports [" + packageName + "] as version ["
						+ hasExport + "] but version [" + iversion + "] was required");
			return hasExport;
		}
		// Do more detailed checks
		String cname = name.substring(packageName.length() + 1) + ".class";
		Enumeration e = bundle.findEntries("/" + packageName.replace('.', '/'), cname, false);
		if (e == null) {
			if (hasExport != null) {
				URL url = checkBundleJarsForClass(bundle, name);
				if (url != null) {
					if (log.isTraceEnabled())
						log.trace("Bundle [" + getBundleName(bundle) + "] contains [" + cname + "] in embedded jar ["
								+ url.toString() + "] but exports the package");
				}
				else {
					if (log.isTraceEnabled())
						log.trace("Bundle [" + getBundleName(bundle) + "] does not contain [" + cname
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
					if (log.isTraceEnabled())
						log.trace("Bundle [" + getBundleName(bundle) + "] contains package [" + packageName
								+ "] and exports it");
				}
				else {
					if (log.isTraceEnabled())
						log.trace("Bundle [" + getBundleName(bundle) + "] contains package [" + packageName
								+ "] but does not export it");
				}

			}
		}
		// Found the resource, check that it is exported.
		else {
			if (hasExport != null) {
				if (log.isTraceEnabled())
					log.trace("Bundle [" + getBundleName(bundle) + "] contains resource [" + cname
							+ "] and it is correctly exported as version [" + hasExport + "]");
				Class c = null;
				try {
					c = bundle.loadClass(name);
				}
				catch (ClassNotFoundException e1) {
                    // Ignored
                }
				if (log.isTraceEnabled())
					log.trace("Bundle [" + getBundleName(bundle) + "] loadClass [" + cname + "] returns [" + c + "]");
			}
			else {
				if (log.isTraceEnabled())
					log.trace("Bundle [" + getBundleName(bundle) + "] contains resource [" + cname
							+ "] but its package is not exported");
			}
		}
		return hasExport;
	}

	private static URL checkBundleJarsForClass(Bundle bundle, String name) {
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
				if (log.isTraceEnabled())
					log.trace("Skipped " + url.toString() + ": " + e1.getMessage());
			}
		}
		return null;
	}

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

	// Pull out a version of the meta-data
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
                            if (vstr.startsWith("\"")) vstr = vstr.substring(1);
                            if (vstr.endsWith("\"")) vstr = vstr.substring(0, vstr.length() - 1);
                            version = Version.parseVersion(vstr);
						}
					}
					return version;
				}
			}
		}
		return null;
	}

	private static String getBundleName(Bundle bundle) {
		Dictionary dict = bundle.getHeaders();
		String name = (String) dict.get(Constants.BUNDLE_NAME);
		String sname = (String) dict.get(Constants.BUNDLE_SYMBOLICNAME);
		return (sname != null ? sname : name) + " (" + bundle.getLocation() + ")";
	}

	protected URL findResource(String name) {
		if (log.isTraceEnabled())
			log.trace("looking for resource " + name);
		URL url = this.backingBundle.getResource(name);

		if (url != null && log.isTraceEnabled())
			log.trace("found resource " + name + " at " + url);
		return url;
	}

	protected Enumeration findResources(String name) throws IOException {
		if (log.isTraceEnabled())
			log.trace("looking for resources " + name);

		Enumeration enm = this.backingBundle.getResources(name);

		if (enm != null && enm.hasMoreElements() && log.isTraceEnabled())
			log.trace("found resource " + name + " at " + this.backingBundle.getLocation());

		return enm;
	}

	public URL getResource(String name) {
		return (bridge == null) ? findResource(name) : super.getResource(name);
	}

	protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz;
        try {
            clazz = findClass(name);
        } catch (ClassNotFoundException e) {
            clazz = bridge.loadClass(name);
        }
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }

	// For testing
	public Bundle getBundle() {
		return backingBundle;
	}

    public String toString() {
		Dictionary dict = backingBundle.getHeaders();
		String bname = dict.get(Constants.BUNDLE_NAME) + "(" + dict.get(Constants.BUNDLE_SYMBOLICNAME) + ")";
        return "BundleDelegatingClassLoader for [" + bname + "]";
    }
}
