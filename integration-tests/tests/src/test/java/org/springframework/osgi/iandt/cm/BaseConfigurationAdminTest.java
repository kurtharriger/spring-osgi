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

package org.springframework.osgi.iandt.cm;

import java.io.FilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationPermission;
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


	protected String[] getTestBundlesNames() {
		// felix configuration admin implementation
		return new String[] { "org.apache.felix, org.apache.felix.configadmin, 1.0.4" };
	}

	protected String[] getBundleContentPattern() {
		List list = new ArrayList();
		CollectionUtils.mergeArrayIntoCollection(super.getBundleContentPattern(), list);
		list.add(BaseConfigurationAdminTest.class.getName().replace('.', '/').concat("*.class"));
		return (String[]) list.toArray(new String[list.size()]);
	}

	protected void onSetUp() throws Exception {
		ServiceReference ref = OsgiServiceReferenceUtils.getServiceReference(bundleContext,
			ConfigurationAdmin.class.getName(), null);
		Assert.notNull(ref, "Configuration Admin not present");
		cm = (ConfigurationAdmin) bundleContext.getService(ref);

		prepareConfiguration();
	}

	/**
	 * Template method for creating a default configuration
	 */
	private void prepareConfiguration() {
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
}
