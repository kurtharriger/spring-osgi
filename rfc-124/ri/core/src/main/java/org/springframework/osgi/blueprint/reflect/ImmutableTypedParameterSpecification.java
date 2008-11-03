package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.TypedParameterSpecification;
import org.osgi.service.blueprint.reflect.Value;

public class ImmutableTypedParameterSpecification extends
		AbstractParameterSpecification implements TypedParameterSpecification {

	private final String typeName;
	
	public ImmutableTypedParameterSpecification(String typeName, Value v) {
		super(v);
		if (null == typeName) {
			throw new IllegalArgumentException("type name cannot be null");
		}
		this.typeName = typeName;
	}
	
	public String getTypeName() {
		return this.typeName;
	}

}
