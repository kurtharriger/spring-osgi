package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.ConstructorInjectionMetadata;
import org.osgi.service.blueprint.reflect.ParameterSpecification;

public class MutableConstructorInjectionMetadata extends ParameterBasedInjectionMetadata implements ConstructorInjectionMetadata {

	public MutableConstructorInjectionMetadata(ParameterSpecification[] paramSpecs) {
		super(paramSpecs);
	}

}
