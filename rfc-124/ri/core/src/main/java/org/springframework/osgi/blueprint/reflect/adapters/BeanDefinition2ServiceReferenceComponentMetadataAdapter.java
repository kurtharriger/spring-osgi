
package org.springframework.osgi.blueprint.reflect.adapters;

import java.util.Collection;
import java.util.Set;

import org.osgi.service.blueprint.reflect.ServiceReferenceComponentMetadata;
import org.springframework.beans.factory.config.BeanDefinition;

class BeanDefinition2ServiceReferenceComponentMetadataAdapter implements ServiceReferenceComponentMetadata {

	private final BeanDefinition beanDef;


	public BeanDefinition2ServiceReferenceComponentMetadataAdapter(BeanDefinition def) {
		this.beanDef = def;
	}

	public Collection getBindingListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set getInterfaceNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getServiceAvailabilitySpecification() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String[] getAliases() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set getExplicitDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
}