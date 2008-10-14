package org.osgi.service.blueprint.reflect;

/**
 * Parameter specification for injection of a parameter identified by its position in the
 * argument list.
 *
 */
public interface IndexedParameterSpecification extends ParameterSpecification {

	/**
	 * The index into the argument list of the parameter to be injected.
	 * 
	 * @return the parameter index, indices start at 0.
	 */
	int getIndex();
}
