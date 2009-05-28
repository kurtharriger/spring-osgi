/*
 * Copyright 2006-2009 the original author or authors.
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

package org.springframework.osgi.blueprint.container;

import java.beans.PropertyEditorSupport;

import org.osgi.service.blueprint.container.Converter;
import org.springframework.util.Assert;

/**
 * Adapter supporting RFC 124 {@link Converter converters} inside Spring bean factories.
 * 
 * @author Costin Leau
 * 
 */
public class ConverterToPropertyEditorAdapter extends PropertyEditorSupport {

	private final Converter converter;

	/**
	 * Constructs a new <code>ConverterPropertyEditorAdapter</code> instance.
	 * 
	 * @param converter RFC 124 converter
	 */
	public ConverterToPropertyEditorAdapter(Converter converter) {
		Assert.notNull(converter, "non-null converter expected");
		this.converter = converter;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	@Override
	public void setValue(Object value) {
		throw new UnsupportedOperationException("Converters not implemented yet");

		// try {
		// super.setValue(converter.convert(value));
		// }
		// catch (Exception ex) {
		// throw new IllegalArgumentException("Cannot perform conversion", ex);
		// }
	}
}