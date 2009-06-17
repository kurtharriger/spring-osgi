/*
 * Copyright (c) OSGi Alliance (2008, 2009). All Rights Reserved.
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
package org.osgi.service.blueprint.reflect;

import java.util.List;

/**
 * A map-based value. This represented as a collection of entries (as it is done in the schema).
 *
 * A map is defined in the <code>map</code> element.
 *
 */
public interface MapMetadata extends NonNullMetadata {
	/**
	 * The key-type specified for map keys, or null if none given
	 *
	 * Defined in the <code>key-type</code> attribute.
	 */
	String getKeyType();

    /**
     * The value-type specified for the array
     *
     * The <code>value-type</code> attribute.
     */
	String getValueType();

    /**
     * The of Metadata objects that describe the value.
     */
	List /*<MapEntry>*/ getEntries();
}
