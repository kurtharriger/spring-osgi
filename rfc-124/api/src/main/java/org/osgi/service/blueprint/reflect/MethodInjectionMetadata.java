package org.osgi.service.blueprint.reflect;


/**
 * Metadata describing a method to be invoked as part of component configuration.
 *
 */
public interface MethodInjectionMetadata {
	
	/**
	 * The name of the method to be invoked.
	 * 
	 * @return the method name, overloaded methods are disambiguated by
	 * parameter specifications.
	 */
	String getName();
	
	/**
	 * The parameter specifications that determine which method to invoke
	 * (in the case of overloading) and what arguments to pass to it.
	 * 
	 * @return an array of parameter specifications, or an empty array if the
	 * method takes no arguments.
	 */
	ParameterSpecification[] getParameterSpecifications();
}
