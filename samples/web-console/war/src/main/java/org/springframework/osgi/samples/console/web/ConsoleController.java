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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.osgi.samples.console.service.BundleDisplayOption;
import org.springframework.osgi.samples.console.service.OsgiConsole;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
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
		// apply default for selected bundle (if needed)
		if (selectionCommand.getBundleId() == null) {
			selectionCommand.setBundleId(console.getDefaultBundleId());
		}

		BundleDisplayOption displayChoice = selectionCommand.getDisplayChoice();
		Bundle bundle = console.getBundle(selectionCommand.getBundleId());
		SearchSpace searchChoice = SearchSpace.BUNDLE;

		model.addAttribute("bundles", listBundles(displayChoice));
		model.addAttribute("bundleInfo", createBundleInfo(bundle, displayChoice));
		model.addAttribute("searchResult", search(bundle, searchChoice, "**/*"));
	}

	/**
	 * Returns a map containing the list of bundles installed in the platform.
	 * Additionally, the method considers how the bundles should be displayed.
	 * 
	 * @param model model associated with the view
	 * @return "bundles" attribute
	 */
	public Map<Long, String> listBundles(BundleDisplayOption displayChoice) {
		Bundle[] bundles = console.listBundles();
		Map<Long, String> map = new LinkedHashMap<Long, String>(bundles.length);
		for (Bundle bundle : bundles) {
			map.put(bundle.getBundleId(), displayChoice.display(bundle));
		}
		return map;
	}

	@ModelAttribute("displayOptions")
	public Map<BundleDisplayOption, String> listingOptions() {
		return BundleDisplayOption.optionsMap();
	}

	public BundleInfo createBundleInfo(Bundle bundle, BundleDisplayOption displayChoice) {
		BundleInfo info = new BundleInfo(bundle);
		addWiring(info);
		addServices(info, displayChoice);
		return info;
	}

	private void addWiring(BundleInfo info) {
		info.addExportedPackages(console.getExportedPackages(info.getBundle()));
		info.addImportedPackages(console.getImportedPackages(info.getBundle()));
	}

	private void addServices(BundleInfo info, BundleDisplayOption displayChoice) {
		for (ServiceReference registeredReference : console.getRegisteredServices(info.getBundle())) {
			info.addRegisteredServices(new BundleInfo.OsgiService(registeredReference, displayChoice));
		}

		for (ServiceReference importedRef : console.getServicesInUse(info.getBundle())) {
			info.addServiceInUse(new BundleInfo.OsgiService(importedRef, displayChoice));
		}
	}

	private Collection<String> search(Bundle bundle, SearchSpace searchChoice, String userPattern) {
		if (!StringUtils.hasText(userPattern)) {
			return Collections.emptyList();
		}

		// strip any prefix specified by the user
		return console.search(bundle, searchChoice.resourcePrefix() + userPattern);
	}
}
