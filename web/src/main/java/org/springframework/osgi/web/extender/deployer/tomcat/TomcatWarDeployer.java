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

import java.io.File;
import java.io.IOException;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.startup.Embedded;
import org.apache.catalina.startup.ExpandWar;
import org.osgi.framework.Bundle;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.web.extender.deployer.internal.util.Utils;
import org.springframework.osgi.web.extender.deployer.support.AbstractWarDeployer;

/**
 * Apache <a href="http://tomcat.apache.org">Tomcat</a> 5.5.x/6.0.x specific
 * war deployer. Unpacks the given bundle into a temporary folder which is then
 * used for deploying the war into the web container.
 * 
 * <p/>The deployer expects the {@link org.apache.catalina.startup.Catalina}
 * instance to be published as an OSGi service under {@link Embedded} class.
 * 
 * @author Costin Leau
 * 
 * @see Context
 * @see Container
 * @see Loader
 */
public class TomcatWarDeployer extends AbstractWarDeployer {

	/** Catalina OSGi service */
	private Embedded serverService;


	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		serverService = (Embedded) Utils.createServerServiceProxy(getBundleContext(), Embedded.class, "tomcat-server");
	}

	protected Object createDeployment(Bundle bundle, String contextPath) throws Exception {
		String docBase = createDocBase(bundle, contextPath);

		Context catalinaContext = serverService.createContext(contextPath, docBase);
		catalinaContext.setLoader(createCatalinaLoader(bundle));
		catalinaContext.setPrivileged(false);
		catalinaContext.setReloadable(false);

		return catalinaContext;
	}

	protected void startDeployment(Object deployment) throws Exception {
		// start web context
		startCatalinaContext((Context) deployment);
	}

	protected void stopDeployment(Bundle bundle, Object deployment) throws Exception {
		Context context = (Context) deployment;
		String docBase = context.getDocBase();
		// remove context
		serverService.removeContext(context);

		if (log.isDebugEnabled())
			log.debug("Cleaning unpacked folder " + docBase);
		// clean unpacked folder
		ExpandWar.delete(new File(docBase));
	}

	private void startCatalinaContext(Context context) {
		Thread currentThread = Thread.currentThread();

		ClassLoader old = currentThread.getContextClassLoader();
		try {
			// TODO: this seemed to be ignored and another TCCL used instead
			//			ClassLoader jasperTCCLLoader = createJasperClassLoader(context.getLoader().getClassLoader());
			currentThread.setContextClassLoader(null);
			getHost().addChild(context);
		}
		finally {
			currentThread.setContextClassLoader(old);
		}
	}

	/**
	 * Creates a dedicated Catalina Loader plus a special, chained, OSGi
	 * classloader.
	 * 
	 * @param bundle
	 * @return
	 */
	private Loader createCatalinaLoader(Bundle bundle) {
		OsgiCatalinaLoader loader = new OsgiCatalinaLoader();
		// create special class loader
		loader.setClassLoader(Utils.createWebAppClassLoader(bundle, Embedded.class));
		return loader;
	}

	private String createDocBase(Bundle bundle, String contextPath) throws IOException {
		File tmpFile = File.createTempFile("tomcat-" + contextPath.substring(1), ".osgi");
		tmpFile.delete();
		tmpFile.mkdir();

		String path = tmpFile.getCanonicalPath();
		if (log.isDebugEnabled())
			log.debug("Unpacking bundle [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] to folder [" + path
					+ "]...");

		Utils.unpackBundle(bundle, tmpFile);

		return path;
	}

	private Container getHost() {
		// get engine
		Container container = serverService.getContainer();
		// now get host
		Container[] children = container.findChildren();
		// pick the first one and associate the context with it
		return children[0];
	}

	protected String getServerInfo() {
		return serverService.getInfo();
	}

}
