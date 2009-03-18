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

package org.springframework.osgi.blueprint.config;

import org.osgi.service.blueprint.convert.Converter;

/**
 * Taken from the TCK.
 * 
 * @author Costin Leau
 */
public class AsianRegionCodeConverter implements Converter {

	private Class targetClass = RegionCode.class;


	public Object convert(Object source) throws Exception {
		if (source instanceof String) {
			return new AsianRegionCode((String) source);
		}
		// we're supposed to throw an exception if we can't convert
		throw new Exception("Unconvertable object type");
	}

	public Class getTargetClass() {
		return targetClass;
	}
}
