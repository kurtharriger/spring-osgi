package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.FieldInjectionMetadata;
import org.osgi.service.blueprint.reflect.Value;

public class MutableFieldInjectionMetadata extends NameValueInjectionMetadata implements FieldInjectionMetadata {

	public MutableFieldInjectionMetadata(String name, Value v) {
		super(name,v);
	}

}
