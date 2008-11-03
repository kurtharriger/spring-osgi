package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.ReferenceNameValue;

public class ReferenceNameValueObject implements ReferenceNameValue {

	private final String name;
	
	public ReferenceNameValueObject(String name) {
		if (null == name) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
	}
	
	public String getReferenceName() {
		return this.name;
	}

}
