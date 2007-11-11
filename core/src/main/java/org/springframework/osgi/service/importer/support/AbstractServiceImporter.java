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

import java.util.ArrayList;
import java.util.List;

import org.springframework.osgi.service.internal.MandatoryDependencyListener;
import org.springframework.osgi.service.internal.ServiceImporter;
import org.springframework.util.Assert;

/**
 * Base class implementing the {@link ServiceImporter} interface. Abstract by
 * default since it doesn't offer any OSGi specific functionality, which have to
 * be supplied by subclasses.
 * 
 * @author Costin Leau
 * 
 */
public abstract class AbstractServiceImporter implements ServiceImporter {

	/** is at least one service required? */
	private boolean mandatory = true;

	private Cardinality cardinality;

	protected List depedencyListeners = new ArrayList(2);

	public void registerListener(MandatoryDependencyListener listener) {
		Assert.notNull(listener);
		depedencyListeners.add(listener);
	}

	public boolean isMandatory() {
		return mandatory;
	}

	/**
	 * The optional cardinality attribute allows a reference cardinality to be
	 * specified (0..1, 1..1, 0..N, or 1..N). The default is '1..X'.
	 * 
	 * @param cardinality
	 */
	public void setCardinality(Cardinality cardinality) {
		Assert.notNull(cardinality);
		this.cardinality = cardinality;
		this.mandatory = Cardinality.isMandatory(this.cardinality);
	}

	/**
	 * Returns the cardinality used by this importer.
	 * 
	 * @return Returns the importer cardinality
	 */
	public Cardinality getCardinality() {
		return cardinality;
	}
}
