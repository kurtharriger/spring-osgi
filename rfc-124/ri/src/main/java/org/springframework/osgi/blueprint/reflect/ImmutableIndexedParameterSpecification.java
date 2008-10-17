package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.IndexedParameterSpecification;
import org.osgi.service.blueprint.reflect.Value;

public class ImmutableIndexedParameterSpecification extends AbstractParameterSpecification implements
		IndexedParameterSpecification {

	private final int index;
	
	public ImmutableIndexedParameterSpecification(int index, Value v) {
		super(v);
		if (index < 0) {
			throw new IllegalArgumentException("index must be >= 0");
		}
		this.index = index;
	}
	
	public int getIndex() {
		return this.index;
	}


}
