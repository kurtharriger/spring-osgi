package org.springframework.osgi.service.importer;

import org.osgi.framework.Filter;

/**
 * Defines a service dependency to be processed by
 * {@link org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext}
 *
 * @author Andy Piper
 */
public interface OsgiServiceImportDependencyDefinition {
	/**
	 * @return the filter to be used for looking up the required service
	 */
	Filter getFilter();

	/**
	 * @return whether or not this dependency is mandatory
	 */
	boolean isMandatory();
}
