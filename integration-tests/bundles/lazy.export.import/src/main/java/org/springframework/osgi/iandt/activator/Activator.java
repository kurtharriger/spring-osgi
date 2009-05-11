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

package org.springframework.osgi.iandt.activator;

import java.awt.Shape;
import java.awt.geom.Area;
import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Activator used for testing lazy (activated) bundles. Publishes a service (and
 * logs a message) on startup.
 * 
 * @author Costin Leau
 */
public class Activator implements BundleActivator {

	private ServiceRegistration registration;


	public void start(BundleContext context) throws Exception {
        String symName = context.getBundle().getSymbolicName();
		Dictionary<Object, Object> props = new Properties();
		props.put("lazy.marker", "true");
		props.put("bundle.sym.name", symName);

		registration = context.registerService(Shape.class.getName(), new Area(), props);
		
		System.out.println("Bundle " + symName + " was activated");
	}

	public void stop(BundleContext context) throws Exception {
		registration.unregister();
	}
}
