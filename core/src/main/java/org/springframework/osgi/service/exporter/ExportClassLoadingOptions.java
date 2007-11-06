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
package org.springframework.osgi.service.exporter;

import org.springframework.core.enums.StaticLabeledEnum;
import org.springframework.core.enums.StaticLabeledEnumResolver;

/**
 * Exporting classloading options constants.
 * 
 * @author Costin Leau
 * 
 */
public class ExportClassLoadingOptions extends StaticLabeledEnum {

	
	/**
	 * The TCCL will not be managed upon service invocation.
	 */
	public static final ExportClassLoadingOptions UNMANAGED = new ExportClassLoadingOptions(0, "unmanaged");

	/**
	 * The TCCL will be set to the service provider upon service invocation.
	 */
	public static final ExportClassLoadingOptions SERVICE_PROVIDER = new ExportClassLoadingOptions(1,
			"service-provider");

	public static ExportClassLoadingOptions resolveEnum(String label) {
		return (ExportClassLoadingOptions) StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(
			ExportClassLoadingOptions.class, label);
	}

	public static ExportClassLoadingOptions resolveEnum(int code) {
		return (ExportClassLoadingOptions) StaticLabeledEnumResolver.instance().getLabeledEnumByCode(
			ExportClassLoadingOptions.class, new Integer(code));
	}

	private ExportClassLoadingOptions(int code, String label) {
		super(code, label);
	}
}
