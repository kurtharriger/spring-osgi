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

package org.springframework.osgi.web.extender.internal.jetty;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.webapp.WebAppClassLoader;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.osgi.OsgiException;
import org.springframework.osgi.service.importer.support.ImportContextClassLoader;
import org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.osgi.web.extender.WarDeployer;
import org.springframework.osgi.web.extender.internal.util.ChainedClassLoader;
import org.springframework.osgi.web.extender.internal.util.Utils;
import org.springframework.util.ObjectUtils;

/**
 * <a href="http://jetty.mortbay.org">Jetty</a> 6.1.x specific war deployer.
 * Since Jetty Server does not use interfaces, CGLIB is required for proxying.
 * 
 * @author Costin Leau
 * 
 */
// TODO: add support for fragments that can start/stop during the life of a war
public class JettyWarDeployer implements WarDeployer {

	private static final String SLASH = "/";

	private static final String JASPER_CLASS = "org.apache.jasper.JspC";

	private static final String JSP_CLASS = "javax.servlet.jsp.JspPage";

	/** logger */
	private static final Log log = LogFactory.getLog(JettyWarDeployer.class);

	/** Jetty system classes */
	// these are loaded by the war parent classloader
	private static final String[] systemClasses = { "java.", "javax.servlet.", "javax.xml.", "org.mortbay." };

	/** Jetty server classes */
	// these aren't loaded by the war classloader
	private static final String[] serverClasses = { "-org.mortbay.jetty.plus.jaas.", "org.mortbay.jetty." };

	/** access to Jetty server service */
	private Server serverService;
	/** OSGi context */
	private BundleContext bundleContext;

	/** web-apps deployed */
	private Map deployed = new LinkedHashMap(4);

	// TODO: make this configurable
	private boolean shouldUnpackWar = true;


	/**
	 * Constructs a new <code>JettyWarDeployer</code> instance.
	 * 
	 */
	public JettyWarDeployer(BundleContext bundleContext) {
		this.bundleContext = bundleContext;

		// make a server proxy first
		OsgiServiceProxyFactoryBean proxyFB = new OsgiServiceProxyFactoryBean();

		// create a bridged classloader so that all the proxy dependencies are considered

		// first between the extender bundle and spring-aop (so that the proxy infrastructure classes are seen)
		// TODO: OSGI-350
		BundleDelegatingClassLoader cl = BundleDelegatingClassLoader.createBundleClassLoaderFor(
			bundleContext.getBundle(), DefaultAopProxyFactory.class.getClassLoader());

		proxyFB.setBeanClassLoader(cl);
		proxyFB.setBundleContext(bundleContext);
		proxyFB.setContextClassLoader(ImportContextClassLoader.UNMANAGED);
		proxyFB.setInterfaces(new Class[] { Server.class });
		proxyFB.setServiceBeanName("jetty-server");
		proxyFB.afterPropertiesSet();

		// get the proxy
		serverService = (Server) proxyFB.getObject();
	}

	public void deploy(Bundle bundle) {
		try {
			if (log.isDebugEnabled())
				log.debug("about to deploy [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] on server "
						+ serverService.getVersion());

			// create wac (and classloader)
			WebAppContext wac = createJettyWebContext(bundle);

			// save web context
			deployed.put(bundle, wac);

			// start web context
			startWebContext(wac);
		}
		catch (Exception ex) {
			log.error("cannot deploy", ex);
			// TODO: add dedicated exception
			throw new OsgiException(ex);
		}
	}

	/**
	 * Creates the Jetty specifc web context for the given OSGi bundle.
	 * 
	 * @param bundle
	 * @return
	 * @throws Exception
	 */
	private WebAppContext createJettyWebContext(Bundle bundle) throws Exception {

		// plug in our custom class only if needed
		WebAppContext wac = (shouldUnpackWar ? new WebAppContext() : new OsgiWebAppContext());

		// create a jetty web app context

		// the server is being used to generate the temp folder (so we have to set it)
		wac.setServer(serverService);
		// set the war string since it's used to generate the temp path
		wac.setWar(OsgiStringUtils.nullSafeName(bundle));
		// same goes for the context path (add leading "/" -> w/o the context will not work)
		wac.setContextPath(SLASH.concat(Utils.getWarContextPath(bundle)));
		// no hot deployment (at least not through directly Jetty)
		wac.setCopyWebDir(false);
		wac.setExtractWAR(shouldUnpackWar);

		//
		// resource settings
		//

		// 1. start with the slow, IO activity
		Resource rootResource = getRootResource(bundle, wac);

		if (log.isDebugEnabled())
			log.debug("the root resources are " + ObjectUtils.nullSafeToString(rootResource.list()));

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

		// respect the servlet spec class-loading contract
		wac.setSystemClasses(systemClasses);
		wac.setServerClasses(serverClasses);

		// create jetty classloader
		ClassLoader jettyClassLoader = Server.class.getClassLoader();

		ClassLoader jspClassLoader = Utils.getClassLoader(JSP_CLASS, jettyClassLoader);

		ClassLoader jasperClassLoader = Utils.getClassLoader(JASPER_CLASS, jettyClassLoader);

		// chain the jasper and jsp classloader (so JSPs can work even if they are not declared)
		ChainedClassLoader chainedCL = new ChainedClassLoader(new ClassLoader[] { jettyClassLoader, jspClassLoader,
			jasperClassLoader });

		// use the bundle classloader as a parent
		ClassLoader bundleClassLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, chainedCL);

		// use the Jetty default classloader to access the WEB-INF/lib and WEB-INF/classes folder
		// TODO: the generic OSGi can be used if the WEB/lib and WEB-INF/classes are part of the class-path
		// TODO: check Jetty behaviour when using a non URL classloader
		WebAppClassLoader warClassLoader = new WebAppClassLoader(bundleClassLoader, wac);
		warClassLoader.setName("Jetty OSGi ClassLoader");

		wac.setClassLoader(warClassLoader);
		// no java 2 loading compliance
		wac.setParentLoaderPriority(false);

		// we're done
		return wac;
	}

	private Resource getRootResource(Bundle bundle, WebAppContext wac) throws IOException {
		// decide whether we unpack or not
		if (shouldUnpackWar) {
			// unpack the war somewhere
			File unpackFolder = unpackBundle(bundle, wac);

			return Resource.newResource(unpackFolder.getCanonicalPath());
		}
		else {
			((OsgiWebAppContext) wac).setBundle(bundle);
			// if it's unpacked, use the bundle API directly
			return new BundleSpaceJettyResource(bundle, SLASH);
		}
	}

	/**
	 * Starts the Jetty web context class.
	 * 
	 * @param wac
	 * @throws Exception
	 */
	private void startWebContext(WebAppContext wac) throws Exception {
		//TODO: is there any way to simplify this ?
		HandlerCollection contexts = (HandlerCollection) serverService.getChildHandlerByClass(ContextHandlerCollection.class);
		if (contexts == null)
			contexts = (HandlerCollection) serverService.getChildHandlerByClass(HandlerCollection.class);
		contexts.addHandler(wac);

		// set the TCCL since it's used internally by Jetty
		Thread current = Thread.currentThread();
		ClassLoader old = current.getContextClassLoader();
		try {
			current.setContextClassLoader(wac.getClassLoader());
			wac.start();
			contexts.start();
		}
		finally {
			current.setContextClassLoader(old);
		}
		if (log.isDebugEnabled())
			log.debug("started context " + wac);
	}

	public void undeploy(Bundle bundle) {
		log.debug("about to undeploy [" + OsgiStringUtils.nullSafeNameAndSymName(bundle) + "] on server "
				+ serverService.getVersion());

		WebAppContext wac = (WebAppContext) deployed.get(bundle);
		// if the bundle has been actually deployed
		if (wac != null)
			try {
				wac.stop();
				// TODO: remove the handler from the server as well
			}
			catch (Exception ex) {
				//TODO: add dedicated exception
				throw new OsgiException(ex);
			}
	}

	private File unpackBundle(Bundle bundle, WebAppContext wac) {

		File extractedWebAppDir = new File(wac.getTempDirectory(), "webapp");
		if (!extractedWebAppDir.exists())
			extractedWebAppDir.mkdir();

		log.info("unpacking bundle " + OsgiStringUtils.nullSafeNameAndSymName(bundle) + " to folder ["
				+ extractedWebAppDir + "] ...");
		// copy the bundle content into the target folder
		Utils.unpackBundle(bundle, extractedWebAppDir);

		return extractedWebAppDir;
	}

}
