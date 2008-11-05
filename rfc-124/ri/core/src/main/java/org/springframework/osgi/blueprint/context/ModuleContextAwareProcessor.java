/*
 * Copyright 2008 the original author or authors.
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
 *
 */

package org.springframework.osgi.blueprint.context;

import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.ModuleContextAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor processor} handling {@link ModuleContextAware}
 * injection.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
public class ModuleContextAwareProcessor implements BeanPostProcessor {

	private final ModuleContext moduleContext;


	/**
	 * Constructs a new <code>ModuleContextAwarePostProcessor</code> instance.
	 * 
	 * @param context module context
	 */
	public ModuleContextAwareProcessor(ModuleContext context) {
		this.moduleContext = context;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ModuleContextAware) {
			((ModuleContextAware) bean).setModuleContext(this.moduleContext);
		}
		return bean;
	}
}