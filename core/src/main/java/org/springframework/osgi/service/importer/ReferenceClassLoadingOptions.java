/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.service.importer;

import org.springframework.core.Constants;
import org.springframework.osgi.service.exporter.ExportClassLoadingOptions;

/**
 * @author Costin Leau
 * 
 */
public abstract class ReferenceClassLoadingOptions extends ExportClassLoadingOptions {

	public static final Constants REFERENCE_CL_OPTIONS = new Constants(ReferenceClassLoadingOptions.class);

	public static final int CLIENT = 2;

	public static int getFromString(String classLoadingManagement) {
		return getFromString(classLoadingManagement, REFERENCE_CL_OPTIONS);
	}
}
