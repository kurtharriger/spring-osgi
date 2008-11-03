package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.ReferenceValue;

public class ReferenceValueObject implements ReferenceValue {
	
	private final String name;
	
	public ReferenceValueObject(String name) {
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
	}

	public String getComponentName() {
		return this.name;
	}

}
