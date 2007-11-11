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
package org.springframework.osgi.service.exporter;

import java.util.Map;

import org.springframework.osgi.service.exporter.OsgiServiceRegistrationListener;


/**
 * @author Costin Leau
 * 
 */
public class SimpleOsgiServiceRegistrationListener implements OsgiServiceRegistrationListener {

	public void registered(Object service, Map serviceProperties) {
		REGISTERED++;
	}

	public void unregistered(Object service, Map serviceProperties) {
		UNREGISTERED++;

	}

	public static int REGISTERED = 0;

	public static int UNREGISTERED = 0;

}
