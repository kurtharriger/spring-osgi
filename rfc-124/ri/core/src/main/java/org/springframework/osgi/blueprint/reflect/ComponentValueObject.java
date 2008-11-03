/**
 * 
 */
package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.ComponentValue;
import org.osgi.service.blueprint.reflect.LocalComponentMetadata;

/**
 * @author acolyer
 *
 */
public class ComponentValueObject implements ComponentValue {

	private final LocalComponentMetadata component;
	
	public ComponentValueObject(LocalComponentMetadata cm) {
		if (null == cm) {
			throw new IllegalArgumentException("component value may not be null");
		}
		this.component = cm;
	}
	
	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.ComponentValue#getComponentMetadata()
	 */
	public LocalComponentMetadata getComponentMetadata() {
		return this.component;
	}

}
