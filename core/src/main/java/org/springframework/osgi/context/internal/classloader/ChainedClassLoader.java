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

package org.springframework.osgi.context.internal.classloader;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.springframework.osgi.util.internal.ClassUtils;
import org.springframework.util.Assert;

/**
 * Chaining class loader implementation that delegates the resource and class
 * loading to a number of class loaders passed in.
 * 
 * <p/> This class loader parent (by default the AppClassLoader) can be
 * specified and will be added automatically as the last entry in the list.
 * 
 * <p/> Additionally, the class space of this class loader can be extended at
 * runtime (by allowing more class loaders to be added).
 * 
 * <strong>Note:</strong>non-OSGi class loaders are considered as special
 * cases. As there are classes that are loaded by the Boot, Ext, App and Fwk
 * ClassLoaders through boot delegation, this implementation tries to identify
 * them and place them last in the chain. Otherwise, these loaders can pull in
 * classes from outside OSGi causing {@link ClassCastException}s.
 * 
 * @author Costin Leau
 */
public class ChainedClassLoader extends ClassLoader {

	/**
	 * list of special class loaders, outside OSGi, that might be used by the
	 * user through boot delegation
	 */
	private static final List knownNonOsgiLoaders = new ArrayList();

	// add the known, non-OSGi class loaders
	// note that the order is important
	static {
		// start with the framework class loader
		// then get all its parents (normally the this should be fwk -> (*) -> app -> ext -> boot)
		// where (*) represents some optional loaders for cases where the framework is embedded

		AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {

				ClassLoader classLoader = getFwkClassLoader();
				addNonOsgiClassLoader(classLoader);
				// get the system class loader
				classLoader = ClassLoader.getSystemClassLoader();
				addNonOsgiClassLoader(classLoader);
				return null;
			}
		});
	}

	/** list of loaders */
	private final List loaders = new ArrayList();

	/** list of special, non-osgi loaders, added by the user */
	private final List nonOsgiLoaders = new ArrayList();

	/** parent class loader */
	private final ClassLoader parent;


	/**
	 * Constructs a new <code>ChainedClassLoader</code> instance.
	 * 
	 * Equivalent to {@link #ChainedClassLoader(ClassLoader[], ClassLoader)}
	 * with the parent class loader initialized to the AppClassLoader
	 * (practically the system bundle class loader).
	 * 
	 * Note that the AppClassLoader can be different then the
	 * {@link #getSystemClassLoader()}, used by
	 * {@link #ChainedClassLoader(ClassLoader[], ClassLoader)} if no parent is
	 * specified.
	 * 
	 * @param loaders array of non-null class loaders
	 */
	public ChainedClassLoader(ClassLoader[] loaders) {
		this(loaders, getFwkClassLoader());
	}

	/**
	 * Constructs a new <code>ChainedClassLoader</code> instance.
	 * 
	 * @param loaders array of non-null class loaders
	 * @param parent parent class loader (can be null)
	 */
	public ChainedClassLoader(ClassLoader[] loaders, ClassLoader parent) {
		super(parent);

		this.parent = parent;

		Assert.notEmpty(loaders);

		synchronized (this.loaders) {
			for (int i = 0; i < loaders.length; i++) {
				ClassLoader classLoader = loaders[i];
				Assert.notNull(classLoader, "null classloaders not allowed");
				addClassLoader(classLoader);
			}
		}
	}

	private static ClassLoader getFwkClassLoader() {
		return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				return Bundle.class.getClassLoader();
			}
		});
	}

	/**
	 * Special static method used during the class initialization.
	 * 
	 * @param classLoader non OSGi class loader
	 */
	private static void addNonOsgiClassLoader(ClassLoader classLoader) {
		while (classLoader != null) {
			synchronized (knownNonOsgiLoaders) {
				if (!knownNonOsgiLoaders.contains(classLoader)) {
					knownNonOsgiLoaders.add(classLoader);
				}
			}
			classLoader = classLoader.getParent();
		}
	}

	public URL getResource(final String name) {
		if (System.getSecurityManager() != null) {
			return (URL) AccessController.doPrivileged(new PrivilegedAction() {

				public Object run() {
					return doGetResource(name);
				}
			});
		}
		else {
			return doGetResource(name);
		}
	}

	private URL doGetResource(String name) {
		URL url = doGetResource(name, loaders);

		if (url != null) {
			return url;
		}
		else {
			url = doGetResource(name, nonOsgiLoaders);
		}

		if (url != null) {
			return url;
		}

		return (parent != null ? parent.getResource(name) : url);
	}

	private URL doGetResource(String name, List classLoaders) {
		URL url = null;
		synchronized (classLoaders) {
			for (int i = 0; i < classLoaders.size(); i++) {
				ClassLoader loader = (ClassLoader) classLoaders.get(i);
				url = loader.getResource(name);
				if (url != null)
					return url;
			}
		}
		return url;
	}

	public Class loadClass(final String name) throws ClassNotFoundException {

		if (System.getSecurityManager() != null) {
			try {
				return (Class) AccessController.doPrivileged(new PrivilegedExceptionAction() {

					public Object run() throws Exception {
						return doLoadClass(name);
					}
				});
			}
			catch (PrivilegedActionException pae) {
				throw (ClassNotFoundException) pae.getException();
			}
		}
		else {
			return doLoadClass(name);
		}
	}

	private Class doLoadClass(String name, List classLoaders) throws ClassNotFoundException {
		Class clazz = null;

		synchronized (classLoaders) {
			for (int i = 0; i < classLoaders.size(); i++) {
				ClassLoader loader = (ClassLoader) classLoaders.get(i);
				try {
					clazz = loader.loadClass(name);
					return clazz;
				}
				catch (ClassNotFoundException e) {
					// keep moving through the class loaders
				}
			}
		}

		return clazz;
	}

	private Class doLoadClass(String name) throws ClassNotFoundException {
		Class clazz = doLoadClass(name, loaders);

		if (clazz != null) {
			return clazz;
		}
		else {
			clazz = doLoadClass(name, nonOsgiLoaders);
		}

		if (clazz != null) {
			return clazz;
		}

		if (parent != null) {
			return parent.loadClass(name);
		}

		else {
			throw new ClassNotFoundException(name);
		}
	}

	/**
	 * Adds a class loader defining the given class, to the chained class loader
	 * space.
	 * 
	 * @param clazz
	 */
	public void addClassLoader(final Class clazz) {
		Assert.notNull(clazz, "a non-null class required");
		addClassLoader((ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				return ClassUtils.getClassLoader(clazz);
			}
		}));
	}

	/**
	 * Adds the given class loader to the existing list.
	 * 
	 * @param classLoader class loader to load classes from
	 */
	public void addClassLoader(ClassLoader classLoader) {
		Assert.notNull(classLoader, "a non-null classLoader required");

		if (!addNonOsgiLoader(classLoader)) {
			addOsgiLoader(classLoader);
		}
	}

	/**
	 * Checks if the given classloader is a known, non-OSGi loader. If it is,
	 * then it is added to a specific list based on the known ordering (ignoring
	 * the user defined one). This is done so that the discovered hierarchy is
	 * respected regardless of the user configuration.
	 * 
	 * @param classLoader
	 * @return true if the class loader was added, false otherwise
	 */
	private boolean addNonOsgiLoader(ClassLoader classLoader) {
		synchronized (nonOsgiLoaders) {
			if (!nonOsgiLoaders.contains(classLoader)) {
				synchronized (knownNonOsgiLoaders) {
					int index = knownNonOsgiLoaders.indexOf(classLoader);
					// add the class loader to the list if there is a match
					if (index >= 0) {
						int insertIndex = 0;
						// but consider the defined order
						for (int i = 0; i < nonOsgiLoaders.size(); i++) {
							int presentLoaderIndex = knownNonOsgiLoaders.indexOf((ClassLoader) nonOsgiLoaders.get(i));
							if (presentLoaderIndex >= 0 && presentLoaderIndex < index) {
								insertIndex = i + 1;
							}
							else {
								continue;
							}
						}
						nonOsgiLoaders.add(insertIndex, classLoader);
						return true;
					}
				}
			}
		}
		return false;
	}

	private void addOsgiLoader(ClassLoader classLoader) {
		synchronized (loaders) {
			if (!loaders.contains(classLoader)) {
				loaders.add(classLoader);
			}
		}
	}
}