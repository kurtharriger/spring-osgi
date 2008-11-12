package org.osgi.service.blueprint.reflect;

import java.util.List;


/**
 * Metadata describing how to instantiate a component instance by
 * invoking one of its constructors.
 */
public interface ConstructorInjectionMetadata {
	
	/**
	 * The parameter specifications that determine which constructor to invoke
	 * and what arguments to pass to it.
	 * 
	 * @return an immutable list of ParameterSpecification, or an empty list if the
	 * default constructor is to be invoked.
	 */
	List /*ParameterSpecification*/ getParameterSpecifications();

}
