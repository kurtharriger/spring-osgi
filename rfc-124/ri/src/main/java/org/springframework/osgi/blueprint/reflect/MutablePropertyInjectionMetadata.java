package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.PropertyInjectionMetadata;
import org.osgi.service.blueprint.reflect.Value;

public class MutablePropertyInjectionMetadata extends
		NameValueInjectionMetadata implements PropertyInjectionMetadata {

	public MutablePropertyInjectionMetadata(String name, Value v) {
		super(name,v);
	}
}
