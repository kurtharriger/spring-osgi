package org.springframework.osgi.service.importer;

import java.util.Set;

/**
 * Interface to be implemented by beans wishing to add to the set of dependencies
 * required by the {@link org.springframework.context.ApplicationContext}. The processed beans are used at the same
 * time in the (@link ApplicationContext} lifecycle as
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor}s, so similar considerations apply
 * when using them.
 *
 * @author Andy Piper
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 */
public interface OsgiServiceImportDependencyFactory {
	/**
	 * Get service dependencies as defined by the bean to be used in determining startup.
	 *
	 * @return a set of OsgiServiceImportDependencyDefinitions
	 */
	public Set getServiceDependencyDefinitions();
}
