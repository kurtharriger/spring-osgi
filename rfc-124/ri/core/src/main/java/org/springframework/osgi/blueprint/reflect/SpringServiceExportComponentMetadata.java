
package org.springframework.osgi.blueprint.reflect;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Default {@link SpringServiceExportComponentMetadata} implementation based on
 * Spring's {@link BeanDefinition}.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
class SpringServiceExportComponentMetadata extends SpringComponentMetadata implements ServiceExportComponentMetadata {

	private final int autoExport = -1;
	private final Set<String> interfaces = null;
	private final int ranking = -1;
	private final Map<String, ? extends Object> serviceProperties = null;


	/**
	 * Constructs a new <code>SpringServiceExportComponentMetadata</code>
	 * instance.
	 * 
	 * @param definition bean definition
	 */
	public SpringServiceExportComponentMetadata(BeanDefinition definition) {
		super(definition);

		MutablePropertyValues propertyValues = definition.getPropertyValues();
	}

	public int getAutoExportMode() {
		throw new UnsupportedOperationException();
	}

	public Value getExportedComponent() {
		throw new UnsupportedOperationException();
	}

	public Set<String> getInterfaceNames() {
		throw new UnsupportedOperationException();
	}

	public int getRanking() {
		throw new UnsupportedOperationException();
	}

	public Collection<RegistrationListenerMetadata> getRegistrationListeners() {
		throw new UnsupportedOperationException();
	}

	public Map<String, ? extends Object> getServiceProperties() {
		throw new UnsupportedOperationException();
	}
}