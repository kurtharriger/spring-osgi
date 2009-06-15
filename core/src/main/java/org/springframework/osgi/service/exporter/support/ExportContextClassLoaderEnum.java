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
package org.springframework.osgi.service.exporter.support;

import org.springframework.osgi.service.importer.support.ImportContextClassLoaderEnum;

/**
 * Enum describing the the possible thread context class loader (tccl) options for exporter OSGi services. If managed,
 * the tccl will be set to the appropriate class loader, on each service call for the duration of the invocation.
 * 
 * <p/> Used by {@link OsgiServiceFactoryBean} for exported services that depend on certain tccl to be set.
 * 
 * @see ImportContextClassLoaderEnum
 * @author Costin Leau
 */
public enum ExportContextClassLoaderEnum {

	/** The tccl will not be managed */
	UNMANAGED,
	/** The tccl will be set to that of the service provider upon service invocation */
	SERVICE_PROVIDER;
}
