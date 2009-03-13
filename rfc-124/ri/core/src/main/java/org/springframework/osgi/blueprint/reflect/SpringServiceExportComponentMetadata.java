
package org.springframework.osgi.blueprint.reflect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.blueprint.reflect.RegistrationListenerMetadata;
import org.osgi.service.blueprint.reflect.ServiceExportComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
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
	private static final String SERVICE_NAME_PROP = "targetBeanName";
	private static final String SERVICE_INSTANCE_PROP = "target";
	private static final String SERVICE_PROPERTIES_PROP = "serviceProperties";
	private static final String LISTENERS_PROP = "listeners";

	private final int autoExport;
	private final Set<String> interfaces;
	private final int ranking;
	private final Value component;
	private final Map<String, ? extends Object> serviceProperties;
	private final Collection<RegistrationListenerMetadata> listeners;


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
			component = ValueFactory.buildValue(MetadataUtils.getValue(pvs, SERVICE_INSTANCE_PROP));
		}

		// interfaces
		Object value = MetadataUtils.getValue(pvs, INTERFACES_PROP);

		if (value != null) {
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
		}
		else {
			interfaces = Collections.<String> emptySet();
		}

		if (pvs.contains(SERVICE_PROPERTIES_PROP)) {
			Map props = (Map) MetadataUtils.getValue(pvs, SERVICE_PROPERTIES_PROP);
			Map convertedProps = new LinkedHashMap(props.size());
			Set<Map.Entry> entrySet = props.entrySet();
			for (Iterator<Map.Entry> iterator = entrySet.iterator(); iterator.hasNext();) {
				Map.Entry entry = iterator.next();
				Object key = entry.getKey();
				Object val = entry.getValue();

				if (key instanceof TypedStringValue) {
					key = ((TypedStringValue) key).getValue();
				}

				if (val instanceof TypedStringValue) {
					val = ((TypedStringValue) val).getValue();
				}
				convertedProps.put(key, val);
			}
			serviceProperties = Collections.unmodifiableMap(convertedProps);
		}
		else {
			serviceProperties = Collections.<String, Object> emptyMap();
		}

		// listeners
		List<RegistrationListenerMetadata> foundListeners = new ArrayList<RegistrationListenerMetadata>(4);
		List<? extends AbstractBeanDefinition> listenerDefinitions = (List<? extends AbstractBeanDefinition>) MetadataUtils.getValue(
			pvs, LISTENERS_PROP);

		if (listenerDefinitions != null) {
			for (AbstractBeanDefinition beanDef : listenerDefinitions) {
				foundListeners.add(new SpringRegistrationListenerMetadata(beanDef));
			}
		}

		listeners = Collections.unmodifiableCollection(foundListeners);
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
		return listeners;
	}

	public Map<String, ? extends Object> getServiceProperties() {
		return serviceProperties;
	}
}