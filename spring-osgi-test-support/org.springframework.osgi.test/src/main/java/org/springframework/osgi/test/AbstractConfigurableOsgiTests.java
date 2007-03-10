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
import org.springframework.osgi.test.platform.FelixPlatform;
import org.springframework.osgi.test.platform.KnopflerfishPlatform;
import org.springframework.osgi.test.platform.OsgiPlatform;

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
	 * <a href="http://www.eclipse.org/equinox">Equinox</a> OSGi platform
	 * constant.
	 */
	public static final String EQUINOX_PLATFORM = "equinox";

	/**
	 * <a href="http://www.knopflerfish.org/">Knopflerfish</a> OSGi platform
	 * constant.
	 */
	public static final String KNOPFLERFISH_PLATFORM = "knopflerfish";

	/**
	 * <a href="http://incubator.apache.org/felix">Felix</a> OSGi platform
	 * constant.
	 */
	public static final String FELIX_PLATFORM = "felix";

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
		String platformName = getPlatformName();

		OsgiPlatform platform = null;
		if (platformName != null) {
			platformName = platformName.toLowerCase();

			if (platformName.indexOf(FELIX_PLATFORM) > -1) {
				platform = new FelixPlatform();

			}
			else if (platformName.indexOf(KNOPFLERFISH_PLATFORM) > -1) {
				platform = new KnopflerfishPlatform();
			}
		}
		
		if (platform == null)
			platform = new EquinoxPlatform();

		// add boot delegation
		// TODO: why is this needed ?
		platform.getConfigurationProperties().setProperty(ORG_OSGI_FRAMEWORK_BOOTDELEGATION,
				getBootDelegationPackageString());

		return platform;
	}

	/**
	 * Indicate what OSGi platform to be used by the test suite. By default,
	 * {@link #OSGI_FRAMEWORK_SELECTOR} system property is used.
	 * 
	 * @return platform
	 */
	protected String getPlatformName() {
		String systemProperty = System.getProperty(OSGI_FRAMEWORK_SELECTOR);

		return (systemProperty == null ? EQUINOX_PLATFORM : systemProperty);
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
	 * 
	 * @return the list of packages the OSGi platform will delegate to the boot
	 * class path Answer an empty list if none.
	 */
	protected List getBootDelegationPackages() {
		List defaults = new ArrayList();
		defaults.add("javax.*");
		defaults.add("org.w3c.*");
		defaults.add("sun.*");
		defaults.add("org.xml.*");
		defaults.add("com.sun.*");
		return defaults;
	}
}
