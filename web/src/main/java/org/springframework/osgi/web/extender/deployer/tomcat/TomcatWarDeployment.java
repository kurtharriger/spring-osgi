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

package org.springframework.osgi.web.extender.deployer.tomcat;

import org.apache.catalina.Context;
import org.osgi.framework.Bundle;
import org.springframework.osgi.web.extender.deployer.OsgiWarDeploymentException;
import org.springframework.osgi.web.extender.deployer.WarDeployment;
import org.springframework.osgi.web.extender.deployer.WarDeploymentContext;
import org.springframework.osgi.web.extender.deployer.internal.support.DefaultWarDeploymentContext;

/**
 * Tomcat-specific deployment class.
 * 
 * @author Costin Leau
 * 
 */
// do all logging in the deployer since that is a public class
class TomcatWarDeployment implements WarDeployment {

	/** active flag */
	private boolean active = true;
	/** catalina context associated with this object */
	private final Context catalinaContext;
	/** deployer entity */
	private final TomcatWarDeployer deployer;
	/** context object */
	private final WarDeploymentContext deploymentContext;


	TomcatWarDeployment(TomcatWarDeployer deployer, Bundle bundle, Context catalinaContext) {
		this.deployer = deployer;
		this.catalinaContext = catalinaContext;

		// create context
		this.deploymentContext = new DefaultWarDeploymentContext(bundle, catalinaContext.getPath(),
			catalinaContext.getServletContext());
	}

	public WarDeploymentContext getDeploymentContext() {
		return deploymentContext;
	}

	public boolean isActive() {
		return active;
	}

	public void undeploy() throws OsgiWarDeploymentException {
		if (!active)
			return;

		active = false;
		deployer.stopCatalinaContext(catalinaContext);
	}

	// package protected method
	Context getCatalinaContext() {
		return catalinaContext;
	}
}
