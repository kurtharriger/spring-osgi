package org.springframework.osgi.blueprint.reflect.adapters;

import java.util.Properties;

import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.factory.config.BeanDefinition;

class BeanDefinition2ServiceExportComponentMetadataAdapter implements
		ServiceExportComponentMetadata {
	
	private final BeanDefinition beanDef;
	
	public BeanDefinition2ServiceExportComponentMetadataAdapter(BeanDefinition def) {
		this.beanDef = def;
	}

	public int getAutoExportMode() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Value getExportedComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getInterfaceNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getRanking() {
		// TODO Auto-generated method stub
		return 0;
	}

	public RegistrationListenerMetadata[] getRegistrationListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	public Properties getServiceProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getAliases() {
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getExplicitDependencies() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}
