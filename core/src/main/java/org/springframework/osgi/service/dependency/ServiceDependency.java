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
package org.springframework.osgi.service.dependency;

/**
 * Generic dependency service import contract. Offers various access on its
 * status such as being mandatory or satisfied.
 * 
 * This interface is used internally for accessing the importer status.
 * 
 * @author Costin Leau
 */
public interface ServiceDependency {

	/**
	 * Does this importer have mandatory dependencies?
	 * 
	 * @return true if dependencies are mandatory, false otherwise.
	 */
	boolean isMandatory();

	/**
	 * Is this importer satisfied? For optional importers, this will always
	 * return true, for mandatory importers, will return true if at least one
	 * service is available or false otherwise.
	 * 
	 * @return
	 */
	boolean isSatisfied();

}
