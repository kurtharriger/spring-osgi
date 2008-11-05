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

package org.springframework.osgi.blueprint.context.support;

import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.ModuleContextAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.osgi.blueprint.context.ApplicationContext2ModuleContextAdapter;
import org.springframework.osgi.blueprint.context.ModuleContextAwareProcessor;

/**
 * {@link BeanFactoryPostProcessor Processor} used at startup, on
 * {@link ApplicationContext} for adding {@link ModuleContext} behaviour.
 * Namely, this processor adds support for {@link ModuleContextAware} beans and
 * for publishing/unpublishing a {@link ModuleContext} that wraps the containing
 * application context.
 * 
 * Normally, this processor is added programmatically to a {@link BeanFactory}
 * before it is being refreshed:
 * 
 * <pre style="code">
 *   beanFactory.addBeanPostProcessor(new ModuleContextBehaviourBeanFactoryPostProcessor(..));
 * </pre>
 * 
 * @author Costin Leau
 */
public class ModuleContextBehaviourBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private final ModuleContext moduleContext;


	public ModuleContextBehaviourBeanFactoryPostProcessor(ModuleContext moduleContext) {
		this.moduleContext = moduleContext;
	}

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// handle ModuleContextAware
		beanFactory.addBeanPostProcessor(new ModuleContextAwareProcessor(moduleContext));
		beanFactory.ignoreDependencyInterface(ModuleContextAware.class);
	}
}
