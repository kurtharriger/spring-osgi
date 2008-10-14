package org.osgi.service.blueprint.reflect;


/**
 * Metadata describing a reference to a service that is to be imported into the module
 * context from the OSGi service registry.
 */
public interface ServiceReferenceComponentMetadata extends ComponentMetadata {

	/**
	 * A matching service is required at all times.
	 */
	public static final int MANDATORY_AVAILABILITY = 1;
	
	/**
	 * A matching service is not required to be present.
	 */
	public static final int OPTIONAL_AVAILABILITY = 2;
	
	/**
	 * Whether or not a matching service is required at all times.
	 * 
	 * @return one of MANDATORY_AVAILABILITY or OPTIONAL_AVAILABILITY
	 */
	int getServiceAvailabilitySpecification();
	
	/**
	 * The interface types that the matching service must support
	 * 
	 * @return an array of type names
	 */
	String[] getInterfaceNames();
	
	/**
	 * The filter expression that a matching service must pass
	 * 
	 * @return filter expression
	 */
	String getFilter();

	/**
	 * The set of listeners registered to receive bind and unbind events for
	 * backing services.
	 * 
	 * @return an array of registered binding listeners, or an empty array
	 * if no listeners are registered.
	 */
	BindingListenerMetadata[] getBindingListeners();
	
}
