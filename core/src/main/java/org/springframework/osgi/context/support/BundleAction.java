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
package org.springframework.osgi.context.support;

import org.osgi.framework.Bundle;

/**
 * Simple strategy class that executes an action on a {@link Bundle}. Normally
 * used with {@link BundleFactoryBean}.
 * 
 * @author Costin Leau
 * 
 */
public interface BundleAction {

	/**
	 * Execute a {@link Bundle} action, based on a configuration and a given
	 * bundle. Normally the returned Bundle is identical to the given one.
	 * 
	 * @param bundle on which the action is executed.
	 * @return updated bundle
	 */
	Bundle execute(Bundle bundle);
}
