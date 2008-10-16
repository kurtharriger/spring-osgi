package org.springframework.osgi.context;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * {@link BeanFactoryPostProcessor} that is invoked late in the application context lifecycle.
 * Specifically it is invoked after service dependencies have been satisfied.
 *
 * @author Andy Piper
 */
public interface DependencyAwareBeanFactoryPostProcessor extends BeanFactoryPostProcessor {
}
