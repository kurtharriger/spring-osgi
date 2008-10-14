
package org.osgi.service.blueprint.reflect;

/**
 * Metadata for a component defined within a given module context.
 * 
 * @see LocalComponentMetadata
 * @see ServiceReferenceComponentMetadata
 * @see ServiceExportComponentMetadata
 */
public interface ComponentMetadata {
	
	/**
	 * The name of the component.
	 * 
	 * @return component name. The component name may be null if this is an anonymously
	 * defined inner component.
	 */
	String getName();
	
	/**
	 * Any aliases by which the component is also known.
	 * 
	 * @return an array of aliases by which the component is known (does not
	 * include the component name as returned by getName()). If the component 
	 * has no aliases then an empty array is returned.
	 */
	String[] getAliases();
	
	/**
	 * The names of any components listed in a "depends-on" attribute for this
	 * component.
	 * 
	 * @return an array of component names for components that we have explicitly
	 * declared a dependency, or an empty array if none.
	 */
	String[] getExplicitDependencies();
}
