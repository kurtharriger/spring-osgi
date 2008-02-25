/*
 * Copyright 2006-2008 the original author or authors.
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

package org.springframework.osgi.context.event;

import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;

/**
 * Event raised when an <code>ConfigurableOsgiBundleApplicationContext</code>
 * <code>refresh</code>
 * method executes successfully.
 * 
 * @author Costin Leau
 */
public class OsgiBundleContextRefreshedEvent extends OsgiBundleApplicationContextEvent {

	/**
	 * Constructs a new <code>OsgiBundleContextRefreshedEvent</code> instance.
	 * 
	 * @param source event source
	 */
	public OsgiBundleContextRefreshedEvent(ConfigurableOsgiBundleApplicationContext source) {
		super(source);
	}

}
