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
package org.springframework.osgi.extender.internal.dependencies;

import org.springframework.osgi.extender.internal.dependencies.shutdown.RecursiveServiceDependencySorter;
import org.springframework.osgi.extender.internal.dependencies.shutdown.ServiceDependencySorter;

/**
 * Shutdown dependency sorted using a recursive algorithm.
 * While it works faster then the comparator it doesn't handle cyclic nodes (yet).
 * @author Costin Leau
 * 
 */
public abstract class RecursiveServiceDependencySorterTest extends AbstractServiceDependencySorterTest {

	protected ServiceDependencySorter createSorter() {
		return new RecursiveServiceDependencySorter();
	}

}
