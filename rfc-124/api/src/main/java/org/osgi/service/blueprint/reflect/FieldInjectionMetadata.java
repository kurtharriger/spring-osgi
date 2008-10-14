package org.osgi.service.blueprint.reflect;

/**
 * Metadata describing a field of a component that is to be injected.
 */
public interface FieldInjectionMetadata {
	
	/**
	 * The name of the field to be injected.
	 * 
	 * @return the field name
	 */
	String getName();

	/**
	 * The value to inject the field with.
	 * 
	 * @return the field value
	 */
	Value getValue();
	
}
