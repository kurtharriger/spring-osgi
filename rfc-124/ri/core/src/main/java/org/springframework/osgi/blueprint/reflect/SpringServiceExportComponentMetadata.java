
package org.springframework.osgi.blueprint.reflect;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.core.enums.StaticLabeledEnumResolver;
import org.springframework.osgi.service.exporter.support.AutoExport;

/**
 * Default {@link SpringServiceExportComponentMetadata} implementation based on
 * Spring's {@link BeanDefinition}.
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
class SpringServiceExportComponentMetadata extends SpringComponentMetadata implements ServiceExportComponentMetadata {

	private static final String AUTO_EXPORT_PROP = "autoExport";
	private static final String RANKING_PROP = "ranking";
	private static final String INTERFACES_PROP = "interfaces";
	private static final String SERVICE_NAME_PROP = "serviceBeanName";
	private static final String SERVICE_PROPERTIES_PROP = "serviceProperties";

	private final int autoExport;
	private final Set<String> interfaces;
	private final int ranking;
	private final Value component;
	private final Map<String, ? extends Object> serviceProperties;


	/**
	 * Constructs a new <code>SpringServiceExportComponentMetadata</code>
	 * instance.
	 * 
	 * @param name bean name
	 * @param definition bean definition
	 */
	public SpringServiceExportComponentMetadata(String name, BeanDefinition definition) {
		super(name, definition);

		MutablePropertyValues pvs = definition.getPropertyValues();

		String autoExp = (String) MetadataUtils.getValue(pvs, AUTO_EXPORT_PROP);
		// convert the internal numbers
		autoExport = ((AutoExport) StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(AutoExport.class, autoExp)).shortValue() + 1;
		// ranking
		if (pvs.contains(RANKING_PROP)) {
			String rank = (String) MetadataUtils.getValue(pvs, RANKING_PROP);
			ranking = Integer.valueOf(rank).intValue();
		}
		else {
			ranking = 0;
		}

		// component
		if (pvs.contains(SERVICE_NAME_PROP)) {
			String compName = (String) MetadataUtils.getValue(pvs, SERVICE_NAME_PROP);
			component = new SimpleReferenceValue(compName);
		}
		else {
			component = null;
		}

		// interfaces
		Object value = MetadataUtils.getValue(pvs, INTERFACES_PROP);

		Set<String> intfs = new LinkedHashSet<String>(4);
		// interface attribute used
		if (value instanceof String) {
			intfs.add((String) value);
		}

		else {
			if (value instanceof Collection) {
				Collection values = (Collection) value;
				for (Iterator iterator = values.iterator(); iterator.hasNext();) {
					TypedStringValue tsv = (TypedStringValue) iterator.next();
					intfs.add(tsv.getValue());
				}
			}
		}
		interfaces = Collections.unmodifiableSet(intfs);

		//		if (pvs.contains(SERVICE_PROPERTIES_PROP)) {
		//			value = MetadataUtils.getValue(pvs, SERVICE_PROPERTIES_PROP);
		//			serviceProperties = (Map<String, ? extends Object>) ValueFactory.buildValue(value);
		//		}
		//		else {
		serviceProperties = Collections.<String, Object> emptyMap();
		//		}

	}

	public int getAutoExportMode() {
		return autoExport;
	}

	public Value getExportedComponent() {
		return component;
	}

	public Set<String> getInterfaceNames() {
		return interfaces;
	}

	public int getRanking() {
		return ranking;
	}

	public Collection<RegistrationListenerMetadata> getRegistrationListeners() {
		throw new UnsupportedOperationException();
	}

	public Map<String, ? extends Object> getServiceProperties() {
		return serviceProperties;
	}
}