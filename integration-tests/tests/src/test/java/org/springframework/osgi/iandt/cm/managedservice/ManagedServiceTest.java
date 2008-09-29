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

package org.springframework.osgi.iandt.cm.managedservice;

import java.util.Dictionary;
import java.util.Properties;

import org.osgi.service.cm.Configuration;
import org.springframework.osgi.iandt.cm.BaseConfigurationAdminTest;

/**
 * @author Costin Leau
 * 
 */
public class ManagedServiceTest extends BaseConfigurationAdminTest {

	private final String ID = getClass().getName();
	private final Dictionary props = new Properties();


	public void testCM() throws Exception {
		Configuration cfg = cm.getConfiguration(ID);
		cfg.update(props);
	}
}
