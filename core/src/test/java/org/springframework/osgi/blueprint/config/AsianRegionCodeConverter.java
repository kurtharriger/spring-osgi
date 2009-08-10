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

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * Taken from the TCK.
 * 
 * @author Costin Leau
 */
public class AsianRegionCodeConverter implements Converter {

	public Object convert(Object source, ReifiedType toType) throws Exception {
		Class toClass = (Class) toType.getRawClass();
		if (source instanceof String
				&& (RegionCode.class.isAssignableFrom(toClass) && toClass.isAssignableFrom(AsianRegionCode.class))) {
			return new AsianRegionCode((String) source);
		}
		// we're supposed to throw an exception if we can't convert
		throw new Exception("Unconvertable object type");
	}

	public boolean canConvert(Object value, ReifiedType toType) {
		Class toClass = (Class) toType.getRawClass();
		return (RegionCode.class.isAssignableFrom(toClass) && toClass.isAssignableFrom(AsianRegionCode.class))
				&& value instanceof String;
	}
}
