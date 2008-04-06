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

package org.springframework.osgi.web.extender.deployer.jetty;

import java.io.File;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;
import org.mortbay.util.IO;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.web.extender.deployer.internal.util.Utils;
import org.springframework.osgi.web.extender.deployer.support.AbstractWarDeployer;

/**
 * <a href="http://jetty.mortbay.org">Jetty</a> 6.1.x+ specific war deployer.
 * Unpacks the given bundle into a temporary folder which is then used for
 * deploying the war into the web container. While the bundle could be used in
 * packed formed by Jetty, for performance reasons and JSP support (through
 * Jasper) an unpack, file-system based format is required.
 * 
 * <p/>The deployer expects the Jetty instance to be published as an OSGi
 * service under {@link Server} class.
 * 
 * @author Costin Leau
 * 
 * @see WebAppContext
 */
public class JettyWarDeployer extends AbstractWarDeployer {

	/** Jetty system classes */
	// these are loaded by the war parent class-loader
	private static final String[] systemClasses = { "java.", "javax.servlet.", "javax.xml.", "org.mortbay." };

	/** Jetty server classes */
	// these aren't loaded by the war class-loader
	private static final String[] serverClasses = { "-org.mortbay.jetty.plus.jaas.", "org.mortbay.jetty." };

	/** access to Jetty server service */
	private Server serverService;


	/**
	 * Constructs a new <code>JettyWarDeployer</code> instance.
	 * 
	 */
	public JettyWarDeployer(BundleContext bundleContext) {
		serverService = (Server) Utils.createServerServiceProxy(bundleContext, Server.class, "jetty-server");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Creates an OSGi-specific Jetty {@link WebAppContext}.
	 */
	protected Object createDeployment(Bundle bundle, String contextPath) throws Exception {
		return createJettyWebContext(bundle, contextPath);
	}

	protected void startDeployment(Object deployment) throws Exception {
		startWebContext((WebAppContext) deployment);
	}

	protected void stopDeployment(Bundle bundle, Object deployment) throws Exception {
		WebAppContext wac = (WebAppContext) deployment;
		stopWebContext(wac);
	}

	/**
	 * Creates the Jetty specifc web context for the given OSGi bundle.
	 * 
	 * @param bundle
	 * @return
	 * @throws Exception
	 */
	private WebAppContext createJettyWebContext(Bundle bundle, String contextPath) throws Exception {

		WebAppContext wac = new WebAppContext();

		// create a jetty web app context

		// the server is being used to generate the temp folder (so we have to set it)
		wac.setServer(serverService);
		// set the war string since it's used to generate the temp path
		wac.setWar(OsgiStringUtils.nullSafeName(bundle));
		// same goes for the context path (add leading "/" -> w/o the context will not work)
		wac.setContextPath(contextPath);
		// no hot deployment (at least not through directly Jetty)
		wac.setCopyWebDir(false);
		wac.setExtractWAR(true);

		//
		// resource settings
		//

		// 1. start with the slow, IO activity
		Resource rootResource = getRootResource(bundle, wac);

		// wac needs access to the WAR root
		// we have to make sure we don't trigger any direct file lookup
		// so instead of calling .setWar()
		// we set the base resource directly
		wac.setBaseResource(rootResource);
		// reset the war setting (so that the base resource is used)
		wac.setWar(null);

		// 
		// class-loading behaviour
		//

		// obey the servlet spec class-loading contract
		wac.setSystemClasses(systemClasses);
		wac.setServerClasses(serverClasses);

		// no java 2 loading compliance
		wac.setParentLoaderPriority(false);
		// create special classloader
		wac.setClassLoader(Utils.createWebAppClassLoader(bundle, Server.class));
		return wac;
	}

	private Resource getRootResource(Bundle bundle, WebAppContext wac) throws Exception {
		// decide whether we unpack or not
		// unpack the war somewhere
		File unpackFolder = unpackBundle(bundle, wac);

		return Resource.newResource(unpackFolder.getCanonicalPath());

		//		else {
		//			((OsgiWebAppContext) wac).setBundle(bundle);
		//			// if it's unpacked, use the bundle API directly
		//			return new BundleSpaceJettyResource(bundle, "/");
		//		}
	}

	/**
	 * Starts the Jetty web context class.
	 * 
	 * @param wac,
	 * @throws Exception
	 */
	private void startWebContext(WebAppContext wac) throws Exception {
		//TODO: is there any way to simplify this ?
		HandlerCollection contexts = getJettyContexts();

		// set the TCCL since it's used internally by Jetty
		Thread current = Thread.currentThread();
		ClassLoader old = current.getContextClassLoader();
		try {
			current.setContextClassLoader(wac.getClassLoader());
			contexts.addHandler(wac);
			wac.start();
			contexts.start();

			if (log.isDebugEnabled())
				log.debug("Started context " + wac);
		}
		finally {
			current.setContextClassLoader(old);
		}
	}

	private void stopWebContext(WebAppContext wac) throws Exception {
		HandlerCollection contexts = getJettyContexts();

		Thread current = Thread.currentThread();
		ClassLoader old = current.getContextClassLoader();
		try {
			current.setContextClassLoader(wac.getClassLoader());
			wac.stop();
			contexts.removeHandler(wac);

			if (log.isDebugEnabled())
				log.debug("Stopped context " + wac);

			// clean up unpacked folder
			Resource rootResource = wac.getBaseResource();
			if (log.isDebugEnabled())
				log.debug("Cleaning unpacked folder " + rootResource);
			IO.delete(rootResource.getFile());
		}
		finally {
			current.setContextClassLoader(old);
		}
	}

	private HandlerCollection getJettyContexts() {
		HandlerCollection contexts = (HandlerCollection) serverService.getChildHandlerByClass(ContextHandlerCollection.class);
		if (contexts == null)
			contexts = (HandlerCollection) serverService.getChildHandlerByClass(HandlerCollection.class);

		return contexts;
	}

	private File unpackBundle(Bundle bundle, WebAppContext wac) throws Exception {
		// Could use Jetty temporary folder
		// File extractedWebAppDir = new File(wac.getTempDirectory(), "webapp");

		File tmpFile = File.createTempFile("jetty-" + wac.getContextPath().substring(1), ".osgi");
		tmpFile.delete();
		tmpFile.mkdir();

		if (log.isDebugEnabled())
			log.debug("Unpacking bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + " to folder ["
					+ tmpFile.getCanonicalPath() + "] ...");
		Utils.unpackBundle(bundle, tmpFile);

		return tmpFile;
	}

	protected String getServerInfo() {
		return "Jetty-" + Server.getVersion();
	}

}
