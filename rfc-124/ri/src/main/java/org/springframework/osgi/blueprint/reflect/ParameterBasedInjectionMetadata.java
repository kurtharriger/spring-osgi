package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.ParameterSpecification;

public abstract class ParameterBasedInjectionMetadata {

	private ParameterSpecification[] params = new ParameterSpecification[0];

	public ParameterBasedInjectionMetadata(ParameterSpecification[] paramSpecs) {
		if (paramSpecs != null) {
			for (ParameterSpecification ps : paramSpecs) {
				if (null == ps) {
					throw new IllegalArgumentException("parameter specification cannot be null");
				}
			}
			this.params = paramSpecs;
		}
	}

	public ParameterSpecification[] getParameterSpecifications() {
		return this.params;
	}

	public void setParameterSpecifiations(ParameterSpecification[] paramSpecs) {
		if (null == paramSpecs) {
			this.params = new ParameterSpecification[0];
		}
		else {
			for (ParameterSpecification ps : paramSpecs) {
				if (null == ps) {
					throw new IllegalArgumentException("parameter specification cannot be null");
				}
			}			
			this.params = paramSpecs;
		}
	}

}