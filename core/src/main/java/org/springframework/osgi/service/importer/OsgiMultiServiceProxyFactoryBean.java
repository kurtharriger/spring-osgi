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
package org.springframework.osgi.service.importer;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Filter;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.core.enums.StaticLabeledEnumResolver;
import org.springframework.osgi.internal.service.ImporterProxy;
import org.springframework.osgi.internal.service.collection.CollectionType;
import org.springframework.osgi.internal.service.collection.OsgiServiceCollection;
import org.springframework.osgi.internal.service.collection.OsgiServiceList;
import org.springframework.osgi.internal.service.collection.OsgiServiceSet;
import org.springframework.osgi.internal.service.collection.OsgiServiceSortedList;
import org.springframework.osgi.internal.service.collection.OsgiServiceSortedSet;
import org.springframework.util.Assert;

/**
 * Specialized single-service proxy creator. Will return a proxy that will
 * select only one OSGi service which matches the configuration criteria. If the
 * selected service goes away, the proxy will search for a replacement.
 * 
 * @see java.util.Collection
 * @see java.util.List
 * @see java.util.Set
 * @see java.util.SortedSet
 * 
 * @author Costin Leau
 * 
 */
public class OsgiMultiServiceProxyFactoryBean extends AbstractOsgiServiceProxyFactoryBean {

	private static final Log log = LogFactory.getLog(OsgiMultiServiceProxyFactoryBean.class);

	private ImporterProxy proxy;

	private Comparator comparator;

	private CollectionType collectionType = CollectionType.LIST;

	public Object getObject() {
		if (!initialized)
			throw new FactoryBeanNotInitializedException();

		if (proxy == null) {
			proxy = createMultiServiceCollection(getUnifiedFilter());
		}

		return proxy;
	}

	public Class getObjectType() {
		return (proxy != null ? proxy.getClass() : collectionType.getCollectionClass());
	}

	public boolean isSatisfied() {
		return (proxy == null ? true : proxy.isSatisfied());
	}

	public void destroy() throws Exception {
		// FIXME: do cleanup
	}

	/**
	 * Create the managed-collection given the existing settings.
	 * 
	 * @param filter OSGi filter
	 * @return importer proxy
	 */
	protected ImporterProxy createMultiServiceCollection(Filter filter) {
		if (log.isDebugEnabled())
			log.debug("creating a multi-value/collection proxy");

		OsgiServiceCollection collection;

		if (CollectionType.COLLECTION.equals(collectionType)) {
			Assert.isNull(comparator, "when specifying a Comparator, a Set or a List have to be used");
			collection = new OsgiServiceCollection(filter, bundleContext, classLoader, mandatory);
		}

		else if (CollectionType.LIST.equals(collectionType)) {
			collection = (comparator == null ? new OsgiServiceList(filter, bundleContext, classLoader, mandatory)
					: new OsgiServiceSortedList(filter, bundleContext, classLoader, comparator, mandatory));
		}
		else if (CollectionType.SET.equals(collectionType)) {
			collection = (comparator == null ? new OsgiServiceSet(filter, bundleContext, classLoader, mandatory)
					: new OsgiServiceSortedSet(filter, bundleContext, classLoader, comparator, mandatory));
		}
		else if (CollectionType.SORTED_LIST.equals(collectionType)) {
			collection = new OsgiServiceSortedList(filter, bundleContext, classLoader, comparator, mandatory);
		}

		else if (CollectionType.SORTED_SET.equals(collectionType)) {
			collection = new OsgiServiceSortedSet(filter, bundleContext, classLoader, comparator, mandatory);
		}

		else {
			throw new IllegalArgumentException("unknown collection type:" + collectionType);
		}

		collection.setListeners(listeners);
		collection.setContextClassLoader(contextClassloader);
		collection.setInterfaces(serviceTypes);
		collection.afterPropertiesSet();

		return collection;
	}

	/**
	 * Set the (optional) comparator for ordering the resulting collection. The
	 * presence of a comparator will force the FactoryBean to use a 'sorted'
	 * collection even though, the specified collection type does not imply
	 * ordering. <p/> Thus, instead of list a sorted list will be created and
	 * instead of a set, a sorted set.
	 * 
	 * @see #setCollectionType(String)
	 * 
	 * @param comparator Comparator (can be null) used for ordering the
	 * resulting collection.
	 */
	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	/**
	 * Set the collection type this FactoryBean will produce. Possible values
	 * are:
	 * <ul>
	 * <li>list - create a list. If a comparator is set, a sorted list will be
	 * created.</li>
	 * <li>set - create a set. If a comparator is set, a sorted set will be
	 * created.</li>
	 * <li>sorted-list - create a sorted list. The ordering will be maintained
	 * either through the comparator (if one is set) or using the natural object
	 * ordering.</li>
	 * <li>sorted-set - create a sorted list. The ordering will be maintained
	 * either through the comparator (if one is set) or using the natural object
	 * ordering.</li>
	 * </ul>
	 * 
	 * @see java.util.Comparator
	 * @see java.lang.Comparable
	 * @see java.util.List
	 * @see java.util.Set
	 * @see java.util.SortedSet
	 * @see #setComparator(Comparator)
	 * 
	 * @param collectionType the collection type as string using one of the
	 * values above.
	 */
	public void setCollectionType(String collectionType) {
		this.collectionType = (CollectionType) StaticLabeledEnumResolver.instance().getLabeledEnumByLabel(
			CollectionType.class, collectionType);
	}

}