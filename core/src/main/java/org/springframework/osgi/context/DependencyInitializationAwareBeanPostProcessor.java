package org.springframework.osgi.context;

import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor} that is invoked early in the application context lifecycle.
 * Specifically it is invoked before service dependencies have been satisfied.
 *
 * @author Andy Piper
 */
public interface DependencyInitializationAwareBeanPostProcessor extends BeanPostProcessor {
}
