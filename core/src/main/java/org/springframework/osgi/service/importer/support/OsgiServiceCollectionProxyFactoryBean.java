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
package org.springframework.osgi.service.importer.support;

import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.springframework.osgi.service.importer.internal.aop.ServiceProxyCreator;
import org.springframework.osgi.service.importer.internal.collection.CollectionProxy;
import org.springframework.osgi.service.importer.internal.collection.OsgiServiceCollection;
import org.springframework.osgi.service.importer.internal.collection.OsgiServiceList;
import org.springframework.osgi.service.importer.internal.collection.OsgiServiceSet;
import org.springframework.osgi.service.importer.internal.collection.OsgiServiceSortedList;
import org.springframework.osgi.service.importer.internal.collection.OsgiServiceSortedSet;
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
public class OsgiServiceCollectionProxyFactoryBean extends AbstractOsgiServiceImportFactoryBean {

	private static final Log log = LogFactory.getLog(OsgiServiceCollectionProxyFactoryBean.class);

	private CollectionProxy proxy;

	private ServiceProxyCreator proxyCreator;

	private Comparator comparator;

	private CollectionType collectionType = CollectionType.LIST;

	public void afterPropertiesSet() {
		super.afterPropertiesSet();

		// create shared proxy creator ( reused for each new service
		// joining the collection)
		proxyCreator = new StaticServiceProxyCreator(getInterfaces(), getBeanClassLoader(), getBundleContext(),
				getContextClassLoader());
	}

	public Object getObject() {
		proxy = (CollectionProxy) super.getObject();
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
	protected Object createProxy() {
		if (log.isDebugEnabled())
			log.debug("creating a multi-value/collection proxy");

		OsgiServiceCollection collection;
		BundleContext bundleContext = getBundleContext();
		ClassLoader classLoader = getBeanClassLoader();
		Filter filter = getUnifiedFilter();

		if (CollectionType.LIST.equals(collectionType)) {
			collection = (comparator == null ? new OsgiServiceList(filter, bundleContext, classLoader, proxyCreator)
					: new OsgiServiceSortedList(filter, bundleContext, classLoader, comparator, proxyCreator));
		}
		else if (CollectionType.SET.equals(collectionType)) {
			collection = (comparator == null ? new OsgiServiceSet(filter, bundleContext, classLoader, proxyCreator)
					: new OsgiServiceSortedSet(filter, bundleContext, classLoader, comparator, proxyCreator));
		}
		else if (CollectionType.SORTED_LIST.equals(collectionType)) {
			collection = new OsgiServiceSortedList(filter, bundleContext, classLoader, comparator, proxyCreator);
		}

		else if (CollectionType.SORTED_SET.equals(collectionType)) {
			collection = new OsgiServiceSortedSet(filter, bundleContext, classLoader, comparator, proxyCreator);
		}

		else {
			throw new IllegalArgumentException("unknown collection type:" + collectionType);
		}

		collection.setRequiredAtStartup(isMandatory());
		collection.setListeners(getListeners());
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
	 * Set the collection type this FactoryBean will produce. Note that if a
	 * comparator is set, a sorted collection will be created even if the
	 * specified type is does not imply ordering. If no comparator is set but
	 * the collection type implies ordering, the natural order of the elements
	 * will be used.
	 * 
	 * @see #setComparator(Comparator)
	 * @see java.lang.Comparable
	 * @see java.util.Comparator
	 * @see CollectionType
	 * 
	 * @param collectionType the collection type as string using one of the
	 * values above.
	 */
	public void setCollectionType(CollectionType collectionType) {
		Assert.notNull(collectionType);
		this.collectionType = collectionType;
	}

	/* override to check proper cardinality - x..N */
	public void setCardinality(Cardinality cardinality) {
		Assert.isTrue(Cardinality.isMultiple(cardinality), "only multiple cardinality ('X..N') accepted");
		super.setCardinality(cardinality);
	}

}