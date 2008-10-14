package org.osgi.service.blueprint.reflect;

/**
 * Metadata describing a parameter of a method or constructor and the
 * value that is to be passed during injection.
 * 
 * @see NamedParameterSpecification
 * @see TypedParameterSpecification
 * @see IndexedParameterSpecification
 */
public interface ParameterSpecification {
	
	/**
	 * The value to inject into the parameter.
	 * 
	 * @return the parameter value
	 */
	Value getValue();
}
