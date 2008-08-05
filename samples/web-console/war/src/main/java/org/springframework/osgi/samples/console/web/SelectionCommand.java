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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.osgi.samples.console.service.BundleListingOptions;

/**
 * @author Costin Leau
 * 
 */
public class SelectionCommand {

	private Map bag = new LinkedHashMap();
	private Long bundleId;
	private BundleListingOptions displayChoice = BundleListingOptions.NAME;


	public Map getBag() {
		return bag;
	}

	public void setBag(Map bag) {
		this.bag = bag;
	}

	public Long getBundleId() {
		return bundleId;
	}

	public void setBundleId(Long bundleId) {
		this.bundleId = bundleId;
	}

	public BundleListingOptions getDisplayChoice() {
		return displayChoice;
	}

	public void setDisplayChoice(BundleListingOptions selectedDisplayOption) {
		this.displayChoice = selectedDisplayOption;
	}

	@Override
	public String toString() {
		return "[bundleId=" + bundleId + "|displayChoice=" + displayChoice.toString()
				+ "]";
	}
}
