/**
 * 
 */
package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.FieldInjectionMetadata;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;
import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;

/**
 * @author acolyer
 *
 */
public class MutableLocalComponentMetadata extends MutableComponentMetadata
		implements LocalComponentMetadata {

	private String className;
	private ConstructorInjectionMetadata constructorMetadata;
	private PropertyInjectionMetadata[] propertyMetadata = new PropertyInjectionMetadata[0];
	private FieldInjectionMetadata[] fieldInjectionMetadata = new FieldInjectionMetadata[0];
	private MethodInjectionMetadata[] methodInjectionMetadata = new MethodInjectionMetadata[0];
	private String initMethod;
	private String destroyMethod;
	private ComponentMetadata factoryComponent;
	private MethodInjectionMetadata factoryMethod;
	private LocalComponentMetadata parentComponent;
	private String scope = SCOPE_SINGLETON;
	private boolean isAbstract = false;
	private boolean isLazy = false;
	
	
	public MutableLocalComponentMetadata(String name) {
		super(name);
		this.constructorMetadata = new MutableConstructorInjectionMetadata(null);
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getClassName()
	 */
	public String getClassName() {
		return this.className;
	}
	
	public void setClassName(String className) {
		this.className = className;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getConstructorInjectionMetadata()
	 */
	public ConstructorInjectionMetadata getConstructorInjectionMetadata() {
		return this.constructorMetadata;
	}
	
	public void setConstructorMetadata(
			ConstructorInjectionMetadata constructorMetadata) {
		this.constructorMetadata = constructorMetadata;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getDestroyMethodName()
	 */
	public String getDestroyMethodName() {
		return this.destroyMethod;
	}
	
	public void setDestroyMethodName(String destroyMethod) {
		this.destroyMethod = destroyMethod;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getFactoryComponent()
	 */
	public ComponentMetadata getFactoryComponent() {
		return this.factoryComponent;
	}
	
	public void setFactoryComponent(ComponentMetadata factoryComponent) {
		this.factoryComponent = factoryComponent;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getFactoryMethodMetadata()
	 */
	public MethodInjectionMetadata getFactoryMethodMetadata() {
		return this.factoryMethod;
	}
	
	public void setFactoryMethodMetadata(MethodInjectionMetadata factoryMethod) {
		this.factoryMethod = factoryMethod;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getFieldInjectionMetadata()
	 */
	public FieldInjectionMetadata[] getFieldInjectionMetadata() {
		return this.fieldInjectionMetadata;
	}
	
	public void setFieldInjectionMetadata(
			FieldInjectionMetadata[] fieldInjectionMetadata) {
		if (fieldInjectionMetadata == null) {
			this.fieldInjectionMetadata = new FieldInjectionMetadata[0];
		}
		else {
			this.fieldInjectionMetadata = fieldInjectionMetadata;
		}
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getInitMethodName()
	 */
	public String getInitMethodName() {
		return this.initMethod;
	}
	
	public void setInitMethodName(String initMethod) {
		this.initMethod = initMethod;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getMethodInjectionMetadata()
	 */
	public MethodInjectionMetadata[] getMethodInjectionMetadata() {
		return this.methodInjectionMetadata;
	}

	public void setMethodInjectionMetadata(
			MethodInjectionMetadata[] methodInjectionMetadata) {
		if (methodInjectionMetadata == null) {
			this.methodInjectionMetadata = new MethodInjectionMetadata[0];
		}
		else {
			this.methodInjectionMetadata = methodInjectionMetadata;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getParent()
	 */
	public LocalComponentMetadata getParent() {
		return this.parentComponent;
	}
	
	public void setParentComponent(LocalComponentMetadata parentComponent) {
		this.parentComponent = parentComponent;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getPropertyInjectionMetadata()
	 */
	public PropertyInjectionMetadata[] getPropertyInjectionMetadata() {
		return this.propertyMetadata;
	}

	public void setPropertyInjectionMetadata(PropertyInjectionMetadata[] propertyMetadata) {
		if (propertyMetadata == null) {
			this.propertyMetadata = new PropertyInjectionMetadata[0];
		}
		else {
			this.propertyMetadata = propertyMetadata;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#getScope()
	 */
	public String getScope() {
		return this.scope;
	}
	
	public void setScope(String scope) {
		if (scope == null) {
			this.scope = SCOPE_SINGLETON;
		}
		else {
			this.scope = scope;
		}
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#isAbstract()
	 */
	public boolean isAbstract() {
		return this.isAbstract;
	}
	
	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.LocalComponentMetadata#isLazy()
	 */
	public boolean isLazy() {
		return this.isLazy;
	}

	public void setLazy(boolean isLazy) {
		this.isLazy = isLazy;
	}
	
}
