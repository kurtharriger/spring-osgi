/**
 * 
 */
package org.springframework.osgi.blueprint.reflect;

import org.osgi.service.blueprint.reflect.TypedStringValue;

/**
 * @author acolyer
 *
 */
public class TypedStringValueObject implements TypedStringValue {
	
	private final String value;
	private final String typeName;
	
	public TypedStringValueObject(String value, String typeName) {
		if (null == value) {
			throw new IllegalArgumentException("value cannot be null");
		}
		if (null == typeName) {
			throw new IllegalArgumentException("type name cannot be null");
		}
		
		this.value = value;
		this.typeName = typeName;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.TypedStringValue#getStringValue()
	 */
	public String getStringValue() {
		return this.value;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.blueprint.reflect.TypedStringValue#getTypeName()
	 */
	public String getTypeName() {
		return this.typeName;
	}

}
