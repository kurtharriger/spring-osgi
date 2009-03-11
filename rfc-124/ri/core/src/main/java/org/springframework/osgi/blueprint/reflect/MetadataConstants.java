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

package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;

/**
 * Holder for various constants used by metadata factories.
 * 
 * @author Costin Leau
 */
interface MetadataConstants {

	// common properties shared across the metadata factories
	static final Class<?> EXPORTER_CLASS = OsgiServiceFactoryBean.class;
	static final Class<?> SINGLE_SERVICE_IMPORTER_CLASS = OsgiServiceFactoryBean.class;
	static final Class<?> MULTI_SERVICE_IMPORTER_CLASS = OsgiServiceFactoryBean.class;

	// component metadata attribute holder (for spring bean definitions)
	static final String SPRING_DM_PREFIX = "spring.osgi.";
	static final String COMPONENT_METADATA_ATTRIBUTE = SPRING_DM_PREFIX + ComponentMetadata.class.getName();
	static final String COMPONENT_NAME = SPRING_DM_PREFIX + "component.name";

	// exporter properties
	static String EXPORTER_RANKING_PROP = "ranking";
	static String EXPORTER_INTFS_PROP = "interfaces";
	static String EXPORTER_PROPS_PROP = "serviceProperties";
	static String EXPORTER_AUTO_EXPORT_PROP = "autoExport";
	static String EXPORTER_TARGET_BEAN_PROP = "targetBean";
	static String EXPORTER_TARGET_BEAN_NAME_PROP = "targetBeanName";

	// importer common properties
	static String IMPORTER_INTFS_PROP = "interfaces";
	static String IMPORTER_FILTER_PROP = "filter";
	static String IMPORTER_CARDINALITY_PROP = "cardinality";

	// single importer 
	static String IMPORTER_BEAN_NAME_PROP = "serviceBeanName";
	static String IMPORTER_TIMEOUT_PROP = "timeout";

	// multi importer
	static String IMPORTER_COLLECTION_PROP = "collectionType";

}
