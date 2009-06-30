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

/**
 * A reference-list Metadata
 */
public interface ReferenceListMetadata extends ServiceReferenceMetadata {

	/**
	 * The List member types must be the proxies to the service objects
	 */
	public static final int USE_SERVICE_OBJECT = 1;

	/**
 	 * The List member types must be Service Reference objects
 	 */
	public static final int USE_SERVICE_REFERENCE = 2;

	/**
	 * Whether the collection will contain service objects, or service
	 * references
	 * Defined in the <code>member-type</code> attribute.
	 *
	 * @return one of USE_SERVICE_OBJECT and USE_SERVICE_REFERENCE
	 * @see #USE_SERVICE_OBJECT
	 * @see #USE_SERVICE_REFERENCE
	 */
	int getMemberType();
}
