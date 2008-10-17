package org.springframework.osgi.blueprint.reflect.adapters;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

public class ComponentMetadata2BeanDefinitionAdapter implements BeanDefinition {
	
	private final ComponentMetadata componentMetadata;
	
	public ComponentMetadata2BeanDefinitionAdapter(ComponentMetadata metadata) {
		this.componentMetadata = metadata;
	}

	public String getBeanClassName() {
		// TODO Auto-generated method stub
		return null;
	}

	public ConstructorArgumentValues getConstructorArgumentValues() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFactoryBeanName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFactoryMethodName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getParentName() {
		// TODO Auto-generated method stub
		return null;
	}

	public MutablePropertyValues getPropertyValues() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getResourceDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getRole() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getScope() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAbstract() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isAutowireCandidate() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isLazyInit() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isSingleton() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setAutowireCandidate(boolean autowireCandidate) {
		// TODO Auto-generated method stub

	}

	public void setBeanClassName(String beanClassName) {
		// TODO Auto-generated method stub

	}

	public void setFactoryBeanName(String factoryBeanName) {
		// TODO Auto-generated method stub

	}

	public void setFactoryMethodName(String factoryMethodName) {
		// TODO Auto-generated method stub

	}

	public void setParentName(String parentName) {
		// TODO Auto-generated method stub

	}

	public void setScope(String scope) {
		// TODO Auto-generated method stub

	}

	public String[] attributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasAttribute(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	public Object removeAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setAttribute(String name, Object value) {
		// TODO Auto-generated method stub

	}

	public Object getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getDescription() {
		return null;
	}

	public BeanDefinition getOriginatingBeanDefinition() {
		return null;
	}

}
