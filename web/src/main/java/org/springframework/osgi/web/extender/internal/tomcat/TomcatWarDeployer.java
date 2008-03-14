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

package org.springframework.osgi.web.extender.internal.tomcat;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.startup.Embedded;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.OsgiException;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.web.extender.WarDeployer;
import org.springframework.osgi.web.extender.internal.util.Utils;

/**
 * <a href="http://tomcat.apache.org">Tomcat</a> 6.0.x specific war deployer.
 * Since Tomcat Catalina classes do not use interfaces, CGLIB is required for
 * proxying the server OSGi service.
 * 
 * @author Costin Leau
 */
public class TomcatWarDeployer implements WarDeployer {

	/** logger */
	private static final Log log = LogFactory.getLog(TomcatWarDeployer.class);

	/** OSGi context */
	private final BundleContext bundleContext;

	/** Catalina OSGi service */
	private final Embedded serverService;

	/** web-apps deployed */
	private Map deployed = new LinkedHashMap(4);

	/** context lock */
	private final Object lock = new Object();


	public TomcatWarDeployer(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		serverService = (Embedded) Utils.createServerServiceProxy(bundleContext, Embedded.class, "tomcat-server");
	}

	public void deploy(Bundle bundle) {
		try {
			if (log.isDebugEnabled())
				log.debug("about to deploy [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] on server "
						+ serverService.getInfo());

			Context context = createCatalinaContext(bundle);

			// save web
			deployed.put(bundle, context);

			// start web context
			startCatalinaContext(context);

		}
		catch (Exception ex) {
			log.error("cannot deploy", ex);
			// TODO: add dedicated exception
			throw new OsgiException(ex);
		}
	}

	public void undeploy(Bundle bundle) {
		log.debug("about to undeploy [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] on server "
				+ serverService.getInfo());

		Context context = (Context) deployed.get(bundle);
		// if the bundle has been actually deployed
		if (context != null)
			try {
				// simply add the context to the engine
				serverService.removeContext(context);

				// TODO: remove the handler from the server as well
			}
			catch (Exception ex) {
				//TODO: add dedicated exception
				throw new OsgiException(ex);
			}
	}

	private void startCatalinaContext(Context context) {
		getHost().addChild(context);
	}

	private Context createCatalinaContext(Bundle bundle) throws IOException {
		String contextPath = Utils.getWarContextPath(bundle);

		String docBase = createDocBase(bundle);

		// TODO: can be the docBase be null ?
		Context catalinaContext = serverService.createContext(contextPath, docBase);
		catalinaContext.setLoader(createCatalinaLoader(bundle));
		catalinaContext.setPrivileged(false);
		catalinaContext.setReloadable(false);

		return catalinaContext;
	}

	/**
	 * Create a dedicated Catalina Loader plus a special, chained, OSGi
	 * classloader.
	 * 
	 * @param bundle
	 * @return
	 */
	private Loader createCatalinaLoader(Bundle bundle) {
		ClassLoader serverLoader = Utils.chainedWebClassLoaders(Embedded.class);
		ClassLoader classLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, serverLoader);
		OsgiCatalinaLoader loader = new OsgiCatalinaLoader();
		loader.setClassLoader(classLoader);
		return loader;
	}

	private String createDocBase(Bundle bundle) throws IOException {
		File tmpFile = File.createTempFile("tomcat-war-", ".osgi");
		tmpFile.delete();
		tmpFile.mkdir();
		Utils.unpackBundle(bundle, tmpFile);

		return tmpFile.getCanonicalPath();
	}

	private Container getHost() {
		// get engine
		Container container = serverService.getContainer();
		// now get host
		Container[] children = container.findChildren();
		// pick the first one and associate the context with it
		return children[0];
	}
}
