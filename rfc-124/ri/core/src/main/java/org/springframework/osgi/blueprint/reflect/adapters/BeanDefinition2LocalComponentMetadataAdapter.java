
package org.springframework.osgi.blueprint.reflect.adapters;

import java.util.Collection;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

class BeanDefinition2LocalComponentMetadataAdapter implements LocalComponentMetadata {

	private final BeanDefinition beanDef;


	public BeanDefinition2LocalComponentMetadataAdapter(BeanDefinition def) {
		this.beanDef = def;
	}

	public String getClassName() {
		return this.beanDef.getBeanClassName();
	}

	public ConstructorInjectionMetadata getConstructorInjectionMetadata() {
		ConstructorArgumentValues cons = this.beanDef.getConstructorArgumentValues();
		return null;//ConstructorArgumentValues2ConstructorInjectionMetadataAdapter(cons);
	}

	public String getDestroyMethodName() {
		return null;
	}

	public ComponentMetadata getFactoryComponent() {
		return null;
	}

	public MethodInjectionMetadata getFactoryMethodMetadata() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getInitMethodName() {
		// TODO Auto-generated method stub
		return null;
	}

	public MethodInjectionMetadata[] getMethodInjectionMetadata() {
		// TODO Auto-generated method stub
		return null;
	}

	public LocalComponentMetadata getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getScope() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isAbstract() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean isLazy() {
		// TODO Auto-generated method stub
		return false;
	}

	public String[] getAliases() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<String> getExplicitDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		return null;
	}

	public Collection getPropertyInjectionMetadata() {
		return null;
	}
}