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

package org.springframework.osgi.web.extender.internal;

import java.util.Map;

import org.osgi.framework.Bundle;
import org.springframework.core.CollectionFactory;
import org.springframework.osgi.web.extender.WarDeployer;

/**
 * Template class offering common functionality for war deployers such as
 * tracking.
 * 
 * @author Costin Leau
 */
public abstract class AbstractWarDeployer implements WarDeployer {

	/** map associating bundles with specific deployment artifacts */
	private final Map deployments = CollectionFactory.createConcurrentMap(4);


	public void deploy(Bundle bundle) throws Exception {
		Object deployment = createDeployment(bundle);
		deployments.put(bundle, deployment);
		startDeployment(deployment);
	}

	public void undeploy(Bundle bundle) throws Exception {
		Object deployment = deployments.remove(bundle);
		if (deployment != null)
			stopDeployment(bundle, deployment);
	}

	/**
	 * Creates and configures the web deployment for the given bundle. The
	 * returned object is used for tracking the bundle and implementations are
	 * free to use whatever appeals to the target environment. The returned
	 * object will be given as argument to {@link #deploy(Bundle)} and
	 * {@link #undeploy(Bundle)}.
	 * 
	 * @param bundle OSGi bundle deployed as war
	 * @return web deployment artifact
	 * @throws Exception if something goes wrong
	 */
	protected Object createDeployment(Bundle bundle) throws Exception {
		return null;
	}

	/**
	 * Starts the deployment artifact.
	 * 
	 * @param deployment web deployment artifact
	 * @throws Exception if something goes wrong
	 */
	protected void startDeployment(Object deployment) throws Exception {
	}

	/**
	 * Stops the deployment artifact.
	 * 
	 * @param bundle OSGi bundle backing the OSGi deployment
	 * @param deployment web deployment artifact
	 * @throws Exception if something goes wrong
	 */
	protected void stopDeployment(Bundle bundle, Object deployment) throws Exception {
	}
}
