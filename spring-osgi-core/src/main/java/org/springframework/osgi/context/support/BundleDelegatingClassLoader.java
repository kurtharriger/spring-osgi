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
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.springframework.aop.framework.Advised;

/**
 * ClassLoader backed by an OSGi bundle. Will use the Bundle class loading.
 * 
 * @author Adrian Colyer
 * @author Andy Piper
 * @author Costin Leau
 * @since 2.0
 */
public class BundleDelegatingClassLoader extends ClassLoader {
	private ClassLoader parent;
	private Bundle backingBundle;

	public BundleDelegatingClassLoader(Bundle aBundle) {
		this(aBundle, Advised.class.getClassLoader());
	}

	public BundleDelegatingClassLoader(Bundle aBundle, ClassLoader parentClassLoader) {
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
			return (parent == null ? true : parent.equals(bundleDelegatingClassLoader.parent));

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
			return this.backingBundle.loadClass(name);
		}
		catch (ClassNotFoundException ex) {
			return parent.loadClass(name);
		}
	}

	protected URL findResource(String name) {
		return this.backingBundle.getResource(name);
	}

	protected Enumeration findResources(String name) throws IOException {
		return this.backingBundle.getResources(name);
	}

	public URL getResource(String name) {
		return (parent == null) ? findResource(name) : super.getResource(name);
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		return (parent == null) ? findClass(name) : super.loadClass(name);
	}

}
