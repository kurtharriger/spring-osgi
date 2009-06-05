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
package org.springframework.osgi.service.importer.support.internal.util;

import java.util.Comparator;

import org.osgi.framework.ServiceReference;
import org.springframework.osgi.service.importer.ServiceReferenceProxy;
import org.springframework.osgi.util.OsgiPlatformDetector;

/**
 * Utility used for comparing ServiceReferences. This class takes into account OSGi 4.0 platforms by providing its own
 * internal comparator.
 * 
 * @author Costin Leau
 */
public abstract class ServiceComparatorUtil {

	protected static final boolean OSGI_41 = OsgiPlatformDetector.isR41();

	protected static final Comparator COMPARATOR =
			(OsgiPlatformDetector.isR41() ? null : new ServiceReferenceComparator());

	public static int compare(ServiceReference left, Object right) {

		if (right instanceof ServiceReferenceProxy) {
			right = ((ServiceReferenceProxy) right).getTargetServiceReference();
		}

		if (left == null && right == null) {
			return 0;
		}

		if (left == null || right == null) {
			throw new ClassCastException("Cannot compare null with a non-null object");
		}

		return (OSGI_41 ? left.compareTo(right) : COMPARATOR.compare(left, right));
	}
}
