
package org.springframework.osgi.blueprint.reflect;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	public List getParameterSpecifications() {
		return Collections.unmodifiableList(Arrays.asList(this.params));
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