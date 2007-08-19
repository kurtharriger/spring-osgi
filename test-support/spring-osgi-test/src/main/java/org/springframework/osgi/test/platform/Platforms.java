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
package org.springframework.osgi.test.platform;

/**
 * Convenience class used for holding the OSGi platforms provided out of the
 * box.
 * 
 * @author Costin Leau
 * 
 */
public abstract class Platforms {

	/**
	 * <a href="http://www.eclipse.org/equinox">Equinox</a> OSGi platform
	 * constant.
	 */
	public static final String EQUINOX = EquinoxPlatform.class.getName();

	/**
	 * <a href="http://www.knopflerfish.org/">Knopflerfish</a> OSGi platform
	 * constant.
	 */
	public static final String KNOPFLEFISH = KnopflerfishPlatform.class.getName();

	/**
	 * <a href="http://felix.apache.org/">Felix</a> OSGi platform constant.
	 */
	public static final String FELIX = FelixPlatform.class.getName();

	/**
	 * <a href="http://www.prosyst.com/products/osgi_se_prof_ed.html">Prosyst
	 * mBedded Professional</a> OSGi platform constant.
	 */
	// public static final String MBS = MBSProPlatform.class.getName();
}
