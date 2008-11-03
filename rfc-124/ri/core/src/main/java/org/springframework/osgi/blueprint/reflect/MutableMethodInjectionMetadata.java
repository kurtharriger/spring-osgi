package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.MethodInjectionMetadata;
import org.osgi.service.blueprint.reflect.ParameterSpecification;

public class MutableMethodInjectionMetadata extends
		ParameterBasedInjectionMetadata implements MethodInjectionMetadata {

	private String name;
	
	public MutableMethodInjectionMetadata(String name, ParameterSpecification[] params) {
		super(params);
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

}
