/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.service.importer.support.internal.controller;

import org.springframework.osgi.service.importer.support.internal.dependency.ImporterStateListener;
import org.springframework.util.Assert;

/**
 * @author Costin Leau
 * 
 */
public class ImporterController implements ImporterInternalActions {

	private ImporterInternalActions executor;


	public ImporterController(ImporterInternalActions executor) {
		Assert.notNull(executor);
		this.executor = executor;
	}

	public void addStateListener(ImporterStateListener stateListener) {
		executor.addStateListener(stateListener);
	}

	public void removeStateListener(ImporterStateListener stateListener) {
		executor.removeStateListener(stateListener);
	}

	public boolean isSatisfied() {
		return executor.isSatisfied();
	}
}
