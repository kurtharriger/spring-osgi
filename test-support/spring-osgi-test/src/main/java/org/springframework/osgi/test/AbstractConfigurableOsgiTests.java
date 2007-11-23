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
package org.springframework.osgi.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.osgi.test.platform.EquinoxPlatform;
import org.springframework.osgi.test.platform.OsgiPlatform;
import org.springframework.osgi.test.platform.Platforms;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract JUnit super class which configures an {@link OsgiPlatform}. <p/>
 * This class offers more hooks for programmatic and declarative configuration
 * of the underlying OSGi platform used when running the test suite.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractConfigurableOsgiTests extends AbstractOsgiTests {

	public AbstractConfigurableOsgiTests() {
		super();
	}

	public AbstractConfigurableOsgiTests(String name) {
		super(name);
	}

	/**
	 * System property for selecting the appropriate OSGi implementation.
	 */
	public static final String OSGI_FRAMEWORK_SELECTOR = "org.springframework.osgi.test.framework";

	/**
	 * 
	 */
	private static final String ORG_OSGI_FRAMEWORK_BOOTDELEGATION = "org.osgi.framework.bootdelegation";

	/**
	 * OSGi platform creation. This method is used to determine and create the
	 * OSGi platform used by the test suite (Equinox by default). See
	 * {@link #getPlatformName()} for an easier alternative.
	 * 
	 * @return the OSGi platform
	 */
	protected OsgiPlatform createPlatform() {
		boolean trace = logger.isTraceEnabled();
		String platformClassName = getPlatformName();

		OsgiPlatform platform = null;
		ClassLoader currentCL = getClass().getClassLoader();

		if (StringUtils.hasText(platformClassName)) {
			if (ClassUtils.isPresent(platformClassName, currentCL)) {
				Class platformClass = ClassUtils.resolveClassName(platformClassName, currentCL);
				if (OsgiPlatform.class.isAssignableFrom(platformClass)) {
					if (trace)
						logger.trace("instantiating platform wrapper...");
					try {
						platform = (OsgiPlatform) platformClass.newInstance();
					}
					catch (Exception ex) {
						logger.warn("cannot instantiate class [" + platformClass + "]; using default");
					}
				}
				else
					logger.warn("class [" + platformClass + "] does not implement " + OsgiPlatform.class.getName()
							+ " interface; falling back to defaults");
			}
			else {
				logger.warn("OSGi platform starter [" + platformClassName + "] not found; using default");
			}

		}
		else
			logger.trace("no platform specified; using default");

		// fallback
		if (platform == null)
			platform = new EquinoxPlatform();

		// add boot delegation
		platform.getConfigurationProperties().setProperty(ORG_OSGI_FRAMEWORK_BOOTDELEGATION,
			getBootDelegationPackageString());

		return platform;
	}

	/**
	 * Indicate what OSGi platform to be used by the test suite. By default,
	 * {@link #OSGI_FRAMEWORK_SELECTOR} system property is used. Subclasses can
	 * override this and provide directly the OSGi platform name.
	 * 
	 * 
	 * @see Platforms
	 * @return platform
	 */
	protected String getPlatformName() {
		String systemProperty = System.getProperty(OSGI_FRAMEWORK_SELECTOR);
		if (logger.isTraceEnabled())
			logger.trace("system property [" + OSGI_FRAMEWORK_SELECTOR + "] has value=" + systemProperty);

		return (systemProperty == null ? Platforms.EQUINOX : systemProperty);
	}

	/**
	 * Return a String representation for the boot delegation packages list.
	 * 
	 * @return
	 */
	private String getBootDelegationPackageString() {
		StringBuffer buf = new StringBuffer();

		for (Iterator iter = getBootDelegationPackages().iterator(); iter.hasNext();) {
			buf.append(((String) iter.next()).trim());
			if (iter.hasNext()) {
				buf.append(",");
			}
		}

		return buf.toString();
	}

	/**
	 * List of OSGi packages that are delegated to the boot classpath. See the
	 * OSGi specification regarding the format of the package string
	 * representation.
	 * 
	 * @return the list of strings representing the packages that the OSGi
	 * platform will delegate to the boot class path.
	 */
	protected List getBootDelegationPackages() {
		List defaults = new ArrayList();
		defaults.add("javax.*");
		defaults.add("org.w3c.*");
		defaults.add("sun.*");
		defaults.add("org.xml.*");
		defaults.add("com.sun.*");
		// FIXME: the JAXP package (for 1.4 VMs) should be discovered in an OSGi
		// manner
		defaults.add("org.apache.xerces.jaxp.*");
		return defaults;
	}
}