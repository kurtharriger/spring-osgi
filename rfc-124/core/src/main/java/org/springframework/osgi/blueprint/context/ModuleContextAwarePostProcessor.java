package org.springframework.osgi.blueprint.context;

import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.ModuleContextAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class ModuleContextAwarePostProcessor implements BeanPostProcessor {
	
	private final ModuleContext moduleContext;
	
	public ModuleContextAwarePostProcessor(ModuleContext context) {
		this.moduleContext = context;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (bean instanceof ModuleContextAware) {
			((ModuleContextAware)bean).setModuleContext(this.moduleContext);
		}
		return bean;
	}

}
