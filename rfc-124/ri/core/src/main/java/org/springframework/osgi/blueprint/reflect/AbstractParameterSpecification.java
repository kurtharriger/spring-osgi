
package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.ParameterSpecification;
import org.osgi.service.blueprint.reflect.Value;

public abstract class AbstractParameterSpecification implements ParameterSpecification {

	private final Value value;


	public AbstractParameterSpecification(Value v) {
		if (null == v) {
			throw new IllegalArgumentException("value cannot be null");
		}

		this.value = v;
	}

	public Value getValue() {
		return this.value;
	}
}