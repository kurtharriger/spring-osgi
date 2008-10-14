package org.osgi.service.blueprint.reflect;


/**
 * Metadata describing how to instantiate a component instance by
 * invoking one of its constructors.
 */
public interface ConstructorInjectionMetadata {
	
	/**
	 * The parameter specifications that determine which constructor to invoke
	 * and what arguments to pass to it.
	 * 
	 * @return an array of parameter specifications, or an empty array if the
	 * default constructor is to be invoked.
	 */
	ParameterSpecification[] getParameterSpecifications();

}
