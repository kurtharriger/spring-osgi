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

package org.springframework.osgi.iandt.cm;

import java.io.File;
import java.io.FilePermission;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.PropertyPermission;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ConfigurationPermission;
import org.osgi.service.cm.ConfigurationPlugin;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.iandt.BaseIntegrationTest;
import org.springframework.osgi.util.OsgiServiceReferenceUtils;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Base class for configuration admin tests.
 * 
 * @author Costin Leau
 * 
 */
public abstract class BaseConfigurationAdminTest extends BaseIntegrationTest {

	protected ConfigurationAdmin cm;
	private final static String CONFIG_DIR = "test-config";

	protected static void initializeDirectory(String dir) {
		File directory = new File(dir);
		remove(directory);
		assertTrue(dir + " directory successfully created", directory.mkdirs());
	}

	private static void remove(File directory) {
		if (directory.exists()) {
			File[] files = directory.listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.isDirectory()) {
					remove(file);
				} else {
					assertTrue(file + " deleted", file.delete());
				}
			}
			assertTrue(directory + " directory successfully cleared", directory.delete());
		}
	}

	protected void preProcessBundleContext(BundleContext context) throws Exception {
		super.preProcessBundleContext(context);
		System.setProperty("felix.cm.dir", CONFIG_DIR);
		initializeDirectory(CONFIG_DIR);
	}

	protected ConfigurableApplicationContext createApplicationContext(String[] locations) {
		ServiceReference ref =
				OsgiServiceReferenceUtils.getServiceReference(bundleContext, ConfigurationAdmin.class.getName(), null);
		Assert.notNull(ref, "Configuration Admin not present");
		ConfigurationAdmin ca = (ConfigurationAdmin) bundleContext.getService(ref);

		try {
			prepareConfiguration(ca);
		} catch (Exception ex) {
			throw new RuntimeException("Cannot prepare Configuration Admin service", ex);
		}

		return super.createApplicationContext(locations);
	}

	protected String[] getTestBundlesNames() {
		// felix configuration admin implementation
		return new String[] { "org.apache.felix, org.apache.felix.configadmin, 1.2.4" };
	}

	protected String[] getBundleContentPattern() {
		List list = new ArrayList();
		CollectionUtils.mergeArrayIntoCollection(super.getBundleContentPattern(), list);
		list.add(BaseConfigurationAdminTest.class.getName().replace('.', '/').concat("*.class"));
		return (String[]) list.toArray(new String[list.size()]);
	}

	protected void onSetUp() throws Exception {
		ServiceReference ref =
				OsgiServiceReferenceUtils.getServiceReference(bundleContext, ConfigurationAdmin.class.getName(), null);
		Assert.notNull(ref, "Configuration Admin not present");
		cm = (ConfigurationAdmin) bundleContext.getService(ref);
	}

	protected void onTearDown() throws Exception {
		Configuration cfgs[] = cm.listConfigurations(null);
		if (cfgs != null) {
			for (int i = 0; i < cfgs.length; i++) {
				Configuration configuration = cfgs[i];
				configuration.delete();
			}
		}
	}

	/**
	 * Template method for initializing the Configuration Admin service <b>before</b> the test application context gets
	 * created.
	 * 
	 * @throws Exception
	 */
	protected void prepareConfiguration(ConfigurationAdmin configAdmin) throws Exception {
	}

	protected List getTestPermissions() {
		List perms = super.getTestPermissions();
		// export package
		perms.add(new AdminPermission("*", AdminPermission.EXECUTE));
		perms.add(new PropertyPermission("*", "read,write"));
		perms.add(new FilePermission("<<ALL FILES>>", "read,delete,write"));
		perms.add(new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE));
		return perms;
	}

	protected void waitForCfgChangeToPropagate(final String pid, final Properties newProperties) throws Exception {
		final Object monitor = new Object();
		final boolean[] receivedEvent = new boolean[] { false };

		// the current implementation of configuration admin seems to be prone to threading errors
		// (especially race conditions where event for updates are cumulated and not propagated properly
		// due to the asynch nature) so this plugin only expects one event really without checking the source
		ConfigurationListener cp = new ConfigurationListener() {

			public void configurationEvent(ConfigurationEvent event) {
				synchronized (monitor) {
					receivedEvent[0] = true;
					monitor.notify();
				}
			}

			public void modifyConfiguration(ServiceReference reference, Dictionary properties) {
				synchronized (monitor) {
					receivedEvent[0] = true;
					monitor.notify();
				}
			}
		};
		Properties props = new Properties();
		props.setProperty(Constants.SERVICE_PID, pid);
		props.setProperty("cm.target", pid);

		bundleContext.registerService(ConfigurationListener.class.getName(), cp, props);

		// update configuration
		Configuration cfg = cm.getConfiguration(pid);
		System.out.println("Updating properties w/ " + newProperties);
		cfg.update(newProperties);
		// wait a bit since the plugin might receive the old configuration if we move too fast...
		Thread.sleep(1000);

		// wait up to 1 minute(s) for the event to propagate
		synchronized (monitor) {
			if (!receivedEvent[0]) {
				// double check if the properties have been already applied before waiting (since then the event is not
				// propagated anymore since it's not
				// considered new)

				monitor.wait(1 * 60 * 1000);
				assertTrue("Configuration " + pid + " hasn't been updated in 5 minutes...", receivedEvent[0]);
			}
		}
	}
}