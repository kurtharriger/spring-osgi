
package org.osgi.service.blueprint.context;

import java.util.Collection;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.reflect.ComponentMetadata;

/**
 * ModuleContext providing access to the components, service exports, and
 * service references of a module. Only bundles in the ACTIVE state may have an
 * associated ModuleContext. A given BundleContext has at most one associated
 * ModuleContext.
 * 
 * An instance of ModuleContext may be obtained from within a module context by
 * implementing the ModuleContextAware interface on a component class.
 * Alternatively you can look up ModuleContext services in the service registry.
 * The Constants.BUNDLE_SYMBOLICNAME and Constants.BUNDLE_VERSION service
 * properties can be used to determine which bundle the published ModuleContext
 * service is associated with.
 * 
 * A ModuleContext implementation must support safe concurrent access. It is
 * legal for the set of named components and component metadata to change
 * between invocations on the same thread if another thread is concurrently
 * modifying the same mutable ModuleContext implementation object.
 * 
 * @see ModuleContextAware
 * @see org.osgi.framework.Constants
 * 
 */
public interface ModuleContext {

	/**
	 * reason code for destroy method callback of a managed service factory
	 * created component, when the component is being disposed because the
	 * corresponding configuration admin object was deleted.
	 */
	static final int CONFIGURATION_ADMIN_OBJECT_DELETED = 1;

	/**
	 * reason code for destroy method callback of a managed service factory
	 * created component, when the component is being disposed because the
	 * bundle is being stopped.
	 */
	static final int BUNDLE_STOPPING = 2;

	/**
	 * Name of the property used to provide the symbolic name of the bundle on
	 * whose behalf a ModuleContext service has been published.
	 */
	static final String SYMBOLIC_NAME_PROPERTY = "osgi.service.blueprint.symbolicname";

	/**
	 * Name of the property used to provide the version of the bundle on whose
	 * behalf a ModuleContext service has been published.
	 */
	static final String VERSION_PROPERTY = "osgi.service.blueprint.version";


	/**
	 * The set of component names recognized by the module context. This set only includes
	 * the primary names of components, not any aliases.
	 *  
	 * @return an immutable set (of Strings) containing the names of all of the components within the
	 * module.
	 */
	Set getComponentNames();
	
	/**
	 * The set of all component names recognized by the module context, including all
	 * aliases.
	 * 
	 * @return an immutable set (of Strings) containing the names of all of the components within the
	 * module, including aliases. 
	 */
	Set getAllComponentNames();

	/**
	 * Get the component instance for a given named component.
	 * 
	 * @param name the name of the component for which the instance is to be
	 * retrieved. Any of the aliases of a component may be used.
	 * 
	 * @return the component instance, the type of the returned object is
	 * dependent on the component definition, and may be determined by
	 * introspecting the component metadata.
	 * 
	 * @throws NoSuchComponentException if the name specified is not the
	 * name of a component within the module.
	 */
	Object getComponent(String name) throws NoSuchComponentException;

	/**
	 * Get the component metadata for a given named component.
	 * 
	 * @param name the name of the component for which the metadata is to be
	 * retrieved. Any of the aliases of a component may be used.
	 * 
	 * @return the component metadata for the component.
	 * 
	 * @throws NoSuchComponentException if the name specified is not the
	 * name of a component within the module.
	 */
	ComponentMetadata getComponentMetadata(String name) throws NoSuchComponentException;

	/**
	 * Get the service reference metadata for every OSGi service referenced by
	 * this module.
	 * 
	 * @return an immutable collection of ServiceReferenceComponentMetadata, with one entry for each referenced service.
	 */
	Collection /*<ServiceReferenceComponentMetadata>*/ getReferencedServicesMetadata();

	/**
	 * Get the service export metadata for every service exported by this
	 * module.
	 * 
	 * @return an immutable collection of ServiceExportComponentMetadata, with one entry for each service export.
	 */
	Collection /*<ServiceExportComponentMetadata>*/ getExportedServicesMetadata();

	/**
	 * Get the metadata for all components defined locally within this module.
	 * 
	 * @return an immutable collection of LocalComponentMetadata, with one entry for each component. 
	 */
	Collection /*<LocalComponentMetadata>*/ getLocalComponentsMetadata();

	/**
	 * Get the bundle context of the bundle this module context is associated
	 * with.
	 * 
	 * @return the module's bundle context
	 */
	BundleContext getBundleContext();
}
