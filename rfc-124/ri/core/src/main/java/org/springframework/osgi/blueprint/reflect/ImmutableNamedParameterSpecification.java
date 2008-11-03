package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.NamedParameterSpecification;
import org.osgi.service.blueprint.reflect.Value;

public class ImmutableNamedParameterSpecification extends
		AbstractParameterSpecification implements NamedParameterSpecification {

	private final String name;
	
	public ImmutableNamedParameterSpecification(String name, Value v) {
		super(v);
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

}
