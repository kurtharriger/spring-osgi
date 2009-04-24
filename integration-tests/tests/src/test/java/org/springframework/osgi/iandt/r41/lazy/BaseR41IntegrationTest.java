/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.iandt.r41.lazy;

import java.lang.reflect.Method;

import org.osgi.framework.Bundle;
import org.springframework.osgi.iandt.BaseIntegrationTest;
import org.springframework.util.ReflectionUtils;

/**
 * Base test for OSGi R4.1 features (mainly lazy bootstrapping).
 * 
 * @author Costin Leau
 */
public abstract class BaseR41IntegrationTest extends BaseIntegrationTest {

	private static final Method START_LAZY_METHOD;
	public static final Integer START_ACTIVATION_POLICY = Integer.valueOf(0x00000002);

	static {
		Method m = null;
		try {
			m = Bundle.class.getMethod("start", new Class[] { int.class });
		}
		catch (Exception ex) {
		}

		START_LAZY_METHOD = m;
	}


	@Override
	protected boolean isDisabledInThisEnvironment(String testMethodName) {
		return (START_LAZY_METHOD == null || !getPlatformName().contains("Equinox"));
	}

	protected String[] getBundleContentPattern() {
		String pkg = getClass().getPackage().getName().replace('.', '/').concat("/");
		String[] patterns = new String[] { BaseR41IntegrationTest.class.getName().replace('.', '/').concat(".class"),
			BaseIntegrationTest.class.getName().replace('.', '/').concat("*.class"), pkg + "**/*" };
		return patterns;
	}

	protected void startBundleLazy(Bundle bundle) {
		if (bundle != null) {
			ReflectionUtils.invokeMethod(START_LAZY_METHOD, bundle, START_ACTIVATION_POLICY);
		}
	}
}