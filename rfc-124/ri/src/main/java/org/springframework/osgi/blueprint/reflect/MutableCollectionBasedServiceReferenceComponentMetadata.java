/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.osgi.blueprint.reflect;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.blueprint.reflect.CollectionBasedServiceReferenceComponentMetadata;
import org.osgi.service.blueprint.reflect.Value;

public class MutableCollectionBasedServiceReferenceComponentMetadata extends
		MutableServiceReferenceComponentMetadata implements
		CollectionBasedServiceReferenceComponentMetadata {
	
	public enum CollectionType {
		LIST, MAP, SET;
	}
	
	private CollectionType collectionType = CollectionType.SET;
	private Value comparator = null;
	private int comparisonBasis = CollectionBasedServiceReferenceComponentMetadata.ORDER_BASIS_SERVICE_REFERENCES;
	private boolean naturalOrdering = true;
	
	public MutableCollectionBasedServiceReferenceComponentMetadata(String name, CollectionType collectionType) {
		super(name);
		this.collectionType = collectionType;
	}

	@SuppressWarnings("unchecked")
	public Class getCollectionType() {
		switch (this.collectionType) {
		case LIST: return List.class;
		case MAP: return Map.class;
		case SET: return Set.class;
		default: throw new IllegalStateException("unknown collection type");
		}
	}

	public Value getComparator() {
		return this.comparator;
	}
	
	public void setComparator(Value comparator) {
		this.comparator = comparator;
	}

	public int getNaturalOrderingComparisonBasis() {
		return this.comparisonBasis;
	}
	
	public void setNaturalOrderingComparisonBasis(int comparisonBasis) {
		if ((comparisonBasis != CollectionBasedServiceReferenceComponentMetadata.ORDER_BASIS_SERVICE_REFERENCES) &&
			(comparisonBasis != CollectionBasedServiceReferenceComponentMetadata.ORDER_BASIS_SERVICES)) {
			throw new IllegalArgumentException("unknown comparisonBasis: " + comparisonBasis);
		}
		this.comparisonBasis = comparisonBasis;
	}

	public boolean isNaturalOrderingBasedComparison() {
		return this.naturalOrdering;
	}
	
	public void setNaturalOrderingBasedComparison(boolean naturalOrdering) {
		this.naturalOrdering = naturalOrdering;
	}

}
