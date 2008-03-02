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

package org.springframework.osgi.web.extender;

import org.osgi.framework.Bundle;

/**
 * OSGi WAR bundle deployer. Implementations are free to use specific
 * environments for the actual deployment process, such as Apache Tomcat, 
 * OSGi HttpService, Jetty or other web containers.
 * 
 * @author Costin Leau
 * 
 */
public interface WarDeployer {

	/**
	 * Deploys the given bundle as a WAR. It is assumed that the given bundle is
	 * a WAR meaning that it contains a <code>WEB-INF/web.xml</code> file in
	 * its bundle space.
	 * 
	 * @param bundle war bundle
	 */
	void deploy(Bundle bundle);

	/**
	 * Un-deploys the given bundle. Undeploying a WAR makes sense only if it has
	 * been previously deployed.
	 * 
	 * @param bundle
	 */
	void undeploy(Bundle bundle);
}
