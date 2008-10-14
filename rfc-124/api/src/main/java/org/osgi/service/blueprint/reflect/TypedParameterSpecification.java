package org.osgi.service.blueprint.reflect;

/**
 * Parameter specification for injection of a parameter by type.
 */
public interface TypedParameterSpecification extends ParameterSpecification {

	/**
	 * The name of the type that the parameter type must be assignable from.
	 * 
	 * @return the parameter type name
	 */
	String getTypeName();
	
}
