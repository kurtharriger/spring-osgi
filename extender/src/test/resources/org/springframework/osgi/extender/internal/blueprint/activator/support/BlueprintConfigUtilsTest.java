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
package org.springframework.osgi.extender.internal.blueprint.activator.support;

import java.util.Dictionary;
import java.util.Properties;

import junit.framework.TestCase;

import org.osgi.framework.Constants;

/**
 * @author Costin Leau
 */
public class BlueprintConfigUtilsTest extends TestCase {

	public void testNoWaitForDependencies() throws Exception {
		Dictionary props = new Properties();
		props.put(Constants.BUNDLE_SYMBOLICNAME, "foo.bar; " + BlueprintConfigUtils.BLUEPRINT_GRACE_PERIOD + ":=false");
		assertFalse(BlueprintConfigUtils.getWaitForDependencies(props));

	}

	public void testWaitForDependencies() throws Exception {
		Dictionary props = new Properties();
		props.put(Constants.BUNDLE_SYMBOLICNAME, "foo.bar; " + BlueprintConfigUtils.BLUEPRINT_GRACE_PERIOD + ":=true");
		assertTrue(BlueprintConfigUtils.getWaitForDependencies(props));
	}

	public void testNoWaitDefinedForDependencies() throws Exception {
		Dictionary props = new Properties();
		props.put(Constants.BUNDLE_SYMBOLICNAME, "foo.bar");
		assertTrue(BlueprintConfigUtils.getWaitForDependencies(props));
	}
}
