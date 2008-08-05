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

package org.springframework.osgi.samples.console.web;

import java.util.Date;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.osgi.samples.console.service.BundleListingOptions;
import org.springframework.osgi.samples.console.service.OsgiConsole;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.osgi.util.OsgiStringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * OSGi console controller. The application main entry point, this class handles
 * the HTTP resources and provides the logic behind the console web page.
 * 
 * @author Costin Leau
 */
@Controller
public class ConsoleController {

	private final OsgiConsole console;
	private Bundle bundle;
	private BundleListingOptions displayChoice = BundleListingOptions.NAME;


	@Autowired
	public ConsoleController(OsgiConsole console) {
		this.console = console;
	}

	/**
	 * Custom handler for the welcome view.
	 */
	@RequestMapping("/console.do")
	public void consoleHandler(@ModelAttribute("selection")
	SelectionCommand selectionCommand, Model model) {
		System.out.println("model selected diplay choice " + selectionCommand.getDisplayChoice());
		// apply default for selected bundle (if needed)
		if (selectionCommand.getBundleId() == null) {
			selectionCommand.setBundleId(console.getDefaultBundleId());
		}
		displayChoice = selectionCommand.getDisplayChoice();
		bundle = console.getBundle(selectionCommand.getBundleId());
		
		model.addAttribute("bundles", listBundles());
		model.addAttribute("bundleInfo", createBundleInfo());
	}

	/**
	 * Returns a map containing the list of bundles installed in the platform.
	 * Additionally, the method considers how the bundles should be displayed.
	 * 
	 * @param model model associated with the view
	 * @return "bundles" attribute
	 */
	public Map<Long, String> listBundles() {
		Bundle[] bundles = console.listBundles();
		Map<Long, String> map = new LinkedHashMap<Long, String>(bundles.length);
		for (Bundle bundle : bundles) {
			map.put(bundle.getBundleId(), displayChoice.display(bundle));
		}
		return map;
	}

	@ModelAttribute("displayOptions")
	public Map<BundleListingOptions, String> listingOptions() {
		return BundleListingOptions.toStringMap();
	}

	public BundleInfo createBundleInfo() {
		bundle = console.getBundle(bundle.getBundleId());
		BundleInfo info = new BundleInfo();
		addHeaders(info);
		addWiring(info);
		addServices(info);
		return info;
	}

	private void addHeaders(BundleInfo info) {
		Dictionary headers = bundle.getHeaders();
		addKeyValueForHeader(Constants.BUNDLE_ACTIVATOR, info, headers);
		addKeyValueForHeader(Constants.BUNDLE_CLASSPATH, info, headers);
		addKeyValueForHeader(Constants.BUNDLE_NAME, info, headers);
		addKeyValueForHeader(Constants.BUNDLE_SYMBOLICNAME, info, headers);
		info.addProperty(Constants.BUNDLE_VERSION, OsgiBundleUtils.getBundleVersion(bundle));
		info.setLocation(bundle.getLocation());
		info.setState(OsgiStringUtils.bundleStateAsString(bundle));
		info.setLastModified(new Date(bundle.getLastModified()));
	}

	private void addKeyValueForHeader(String headerName, BundleInfo info, Dictionary headers) {
		info.addProperty(headerName, headers.get(headerName));
	}

	private void addWiring(BundleInfo info) {
		info.addExportedPackages(console.getExportedPackages(bundle));
		info.addImportedPackages(console.getImportedPackages(bundle));
	}

	private void addServices(BundleInfo info) {
	}
}
