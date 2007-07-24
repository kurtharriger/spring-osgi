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
package org.springframework.osgi.service.collection.comparator;

import java.util.Comparator;

import org.osgi.framework.Constants;
import org.springframework.osgi.service.ServiceReferenceAware;

/**
 * Default comparator for sorted collections. It uses the service id property of
 * an OSGi service to determine the order. Thus, by using this comparator, the
 * services added to a collection will be sorted in the order in which they are
 * published on the OSGi platform.
 * 
 * @see Comparator
 * @author Costin Leau
 * 
 */
public class OsgiServiceIdComparator implements Comparator {

	/*
	 * (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Object o1, Object o2) {
		ServiceReferenceAware obj1 = (ServiceReferenceAware) o1;
		ServiceReferenceAware obj2 = (ServiceReferenceAware) o2;

		Long id1 = (Long) obj1.getServiceProperties().get(Constants.SERVICE_ID);
		Long id2 = (Long) obj2.getServiceProperties().get(Constants.SERVICE_ID);

		return id1.compareTo(id2);
	}

	public boolean equals(Object obj) {
		return (this == obj || obj instanceof OsgiServiceIdComparator);
	}

}
