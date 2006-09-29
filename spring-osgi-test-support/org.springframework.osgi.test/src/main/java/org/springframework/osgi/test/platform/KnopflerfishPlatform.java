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
 */
package org.springframework.osgi.test.platform;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.knopflerfish.framework.Framework;
import org.osgi.framework.BundleContext;

/**
 * Knopflerfish 2.x Platform.
 * 
 * @author Costin Leau
 * 
 */
public class KnopflerfishPlatform implements OsgiPlatform {

	/**
	 * Not used at the moment
	 */
	private String[] ARGS = new String[] { "-init", "-launch" };
	
	private Properties SYSTEM_PROPS = new Properties();

	private BundleContext context;
	private Framework framework;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.OsgiPlatform#getBundleContext()
	 */
	public BundleContext getBundleContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.OsgiPlatform#start()
	 */
	public void start() throws Exception {
		SYSTEM_PROPS.put("org.knopflerfish.framework.bundlestorage", "memory");
		SYSTEM_PROPS.put("org.knopflerfish.startlevel.use", "true");

		for (Iterator iter = SYSTEM_PROPS.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			System.setProperty((String) entry.getKey(), (String) entry.getValue());
		}
		
		framework = new Framework(this);
		framework.launch(0);
		context = framework.getSystemBundleContext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.osgi.test.OsgiPlatform#stop()
	 */
	public void stop() throws Exception {
		framework.shutdown();
	}
}
