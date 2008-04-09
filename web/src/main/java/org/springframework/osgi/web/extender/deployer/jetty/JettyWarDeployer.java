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
import java.io.IOException;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;
import org.mortbay.util.IO;
import org.osgi.framework.Bundle;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.web.extender.deployer.OsgiWarDeploymentException;
import org.springframework.osgi.web.extender.deployer.WarDeployment;
import org.springframework.osgi.web.extender.deployer.internal.util.Utils;
import org.springframework.osgi.web.extender.deployer.support.AbstractWarDeployer;
import org.springframework.util.Assert;

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
 * @see WebAppContext
 * 
 * @author Costin Leau
 * 
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


	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		serverService = (Server) Utils.createServerServiceProxy(getBundleContext(), Server.class, "jetty-server");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Creates an OSGi-specific Jetty war deployer.
	 */
	protected WarDeployment createDeployment(Bundle bundle, String contextPath) throws Exception {
		WebAppContext wac = createJettyWebContext(bundle, contextPath);
		JettyWarDeployment deployment = new JettyWarDeployment(this, bundle, wac);

		return deployment;
	}

	protected void startDeployment(WarDeployment deployment) throws Exception {
		Assert.isInstanceOf(JettyWarDeployment.class, deployment, "Wrong type of deployment used");
		startWebAppContext(((JettyWarDeployment) deployment).getWebAppContext());
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
		// 1. resource settings
		//

		// start with the slow, IO activity
		Resource rootResource = getRootResource(bundle, wac);

		// wac needs access to the WAR root
		// we have to make sure we don't trigger any direct file lookup
		// so instead of calling .setWar()
		// we set the base resource directly
		wac.setBaseResource(rootResource);
		// reset the war setting (so that the base resource is used)
		wac.setWar(null);

		// 
		// 2. class-loading behaviour
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
	private void startWebAppContext(WebAppContext wac) throws Exception {
		HandlerCollection contexts = getJettyContexts();

		// set the TCCL since it's used internally by Jetty
		Thread current = Thread.currentThread();
		ClassLoader old = current.getContextClassLoader();
		try {
			current.setContextClassLoader(wac.getClassLoader());
			contexts.addHandler(wac);
			wac.start();
			contexts.start();
		}
		finally {
			current.setContextClassLoader(old);
		}
	}

	/**
	 * Stops the given context.
	 * 
	 * @param wac
	 * @throws Exception
	 */
	// package protected method accessible only to the JettyWarDeployment
	void stopWebAppContext(WebAppContext wac) throws OsgiWarDeploymentException {

		Resource rootResource = wac.getBaseResource();
		String contextPath = wac.getContextPath();

		String messageEnding = "context [" + contextPath + "] from server " + getServerInfo();

		log.info("About to undeploy " + messageEnding);

		HandlerCollection contexts = getJettyContexts();

		Thread current = Thread.currentThread();
		ClassLoader old = current.getContextClassLoader();
		try {
			current.setContextClassLoader(wac.getClassLoader());
			wac.stop();
			contexts.removeHandler(wac);
			log.info("Context [" + contextPath + "] undeployed successfully from server " + getServerInfo());
		}
		catch (Exception ex) {
			throw new OsgiWarDeploymentException("Cannot undeploy " + messageEnding, ex);
		}
		finally {
			current.setContextClassLoader(old);

			// clean up unpacked folder
			if (log.isDebugEnabled())
				log.debug("Cleaning unpacked folder " + rootResource);
			try {
				IO.delete(rootResource.getFile());
			}
			catch (IOException ex) {
				// it's clean up so there is nothing else we can do
				// log the error but ignore it otherwise
				log.warn("Could not clean unpacked folder for " + messageEnding, ex);
			}
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
