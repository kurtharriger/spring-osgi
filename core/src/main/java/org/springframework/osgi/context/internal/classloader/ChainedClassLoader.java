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

import org.springframework.osgi.util.internal.ClassUtils;
import org.springframework.util.Assert;

/**
 * Chaining class loader implementation that delegates the resource and class
 * loading to a number of class loaders passed in.
 * 
 * <p/> Additionally, the class space of this class loader can be extended at
 * runtime (by allowing more classloaders to be added).
 * 
 * @author Costin Leau
 */
public class ChainedClassLoader extends ClassLoader {

	private final List loaders = new ArrayList();
	/** index at which new classloader are added to */
	private int index = 0;

	private final ClassLoader systemClassLoader;


	public ChainedClassLoader(ClassLoader[] loaders) {
		Assert.notEmpty(loaders);
		for (int i = 0; i < loaders.length; i++) {
			ClassLoader classLoader = loaders[i];
			Assert.notNull(classLoader, "null classloaders not allowed");
			this.loaders.add(classLoader);
		}

		systemClassLoader = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				return ClassLoader.getSystemClassLoader();
			}
		});
	}

	public URL getResource(final String name) {
		synchronized (loaders) {
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
	}

	private URL doGetResource(String name) {
		URL url = null;
		for (int i = 0; i < loaders.size(); i++) {
			ClassLoader loader = (ClassLoader) loaders.get(i);
			url = loader.getResource(name);
			if (url != null)
				return url;
		}
		return url;
	}

	public Class loadClass(final String name) throws ClassNotFoundException {

		synchronized (loaders) {
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
	}

	private Class doLoadClass(String name) throws ClassNotFoundException {
		Class clazz = null;

		for (int i = 0; i < loaders.size(); i++) {
			ClassLoader loader = (ClassLoader) loaders.get(i);
			try {
				clazz = loader.loadClass(name);
				return clazz;
			}
			catch (ClassNotFoundException e) {
				// keep moving through the class loaders
			}
		}
		throw new ClassNotFoundException(name);
	}

	/**
	 * Adds the classloader defining the given class.
	 * 
	 * @param clazz
	 */
	public void addClassLoader(final Class clazz) {
		addClassLoader((ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

			public Object run() {
				return ClassUtils.getClassLoader(clazz);
			}
		}));
	}

	/**
	 * Adds the given classloader to the existing list. Note that since this
	 * classloader is used internally for AOP purposes, the classloader will
	 * always be added to
	 * 
	 * @param classLoader
	 */
	public void addClassLoader(ClassLoader classLoader) {
		synchronized (loaders) {
			if (!loaders.contains(classLoader)) {
				// special case - add the system classloader last since otherwise you can run into CCE problems 
				if (systemClassLoader.equals(classLoader)) {
					loaders.add(classLoader);
				}
				else {
					loaders.add(++index, classLoader);
				}
			}
		}
	}
}
