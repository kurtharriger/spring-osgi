/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.blueprint.convert;

import org.osgi.service.blueprint.convert.Converter;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

/**
 * Spring {@link PropertyEditorRegistrar registrar} that handles the
 * registration of RFC 124 {@link Converter converters} inside a bean factory.
 * 
 * @author Costin Leau
 * 
 */
public class CoverterPropertyEditorRegistrar implements PropertyEditorRegistrar {

	private final Converter[] converters;


	/**
	 * Constructs a new <code>CoverterPropertyEditorRegistrar</code> instance.
	 * 
	 * @param converters
	 */
	public CoverterPropertyEditorRegistrar(Converter[] converters) {
		this.converters = (converters == null ? new Converter[0] : converters);
	}

	public void registerCustomEditors(PropertyEditorRegistry registry) {
		for (Converter converter : converters) {
			registry.registerCustomEditor(converter.getTargetClass(), new ConverterPropertyEditorAdapter(converter));
		}
	}
}
