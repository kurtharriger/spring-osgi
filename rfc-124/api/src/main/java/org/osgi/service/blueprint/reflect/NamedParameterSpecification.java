package org.osgi.service.blueprint.reflect;

/**
 * Parameter specification for injection of a parameter by name.
 *
 */
public interface NamedParameterSpecification extends ParameterSpecification {
	
	/**
	 * The name of the parameter to be injected.
	 * 
	 * @return the parameter name
	 */
	String getName();

}
