/*
 * Copyright 2002-2007 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.osgi.internal.propertyeditors;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

import org.osgi.framework.ServiceReference;
import org.springframework.osgi.service.importer.ServiceReferenceAccessor;

/**
 * {@link PropertyEditor} that converts an &lt;osgi:reference&gt; element into a
 * {@link ServiceReference}. That is, it allows conversion between a
 * Spring-managed OSGi service to a Spring-managed ServiceReference.
 * 
 * @see ServiceReferenceDelegate
 * @see ServiceReferenceAccessor
 * 
 * @author Costin Leau
 * 
 */
public class SingleServiceReferenceEditor extends PropertyEditorSupport {

	/**
	 * Convert the given text value to a ServiceReference.
	 */
	public void setAsText(String text) throws IllegalArgumentException {
		throw new IllegalArgumentException("this property editor works only with "
				+ ServiceReferenceAccessor.class.getName());
	}

	/**
	 * Convert the given value to a ServiceReference.
	 */
	public void setValue(Object value) {
		if (value == null) {
			super.setValue(value);
			return;
		}

		if (value instanceof ServiceReferenceAccessor) {
			super.setValue(new ServiceReferenceDelegate((ServiceReferenceAccessor) value));
			return;
		}
		throw new IllegalArgumentException("expected a service of type " + ServiceReferenceAccessor.class.getName());
	}

	/**
	 * This implementation returns <code>null</code> to indicate that there is
	 * no appropriate text representation.
	 */
	public String getAsText() {
		return null;
	}

}
