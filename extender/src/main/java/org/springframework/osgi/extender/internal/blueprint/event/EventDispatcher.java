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

package org.springframework.osgi.extender.internal.blueprint.event;

import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;

/**
 * Dispatcher of {@link OsgiBundleApplicationContextEvent events}. Normally used as an adapter to other event
 * infrastructure such as {@link org.springframework.context.EventAdmin}. If the need arises, this interface might be
 * promoted and moved into Spring DM core.
 * 
 * @author Costin Leau
 */
interface EventDispatcher {

	void beforeClose(ConfigurableOsgiBundleApplicationContext context);

	void beforeRefresh(ConfigurableOsgiBundleApplicationContext context);

	void afterClose(ConfigurableOsgiBundleApplicationContext context);

	void afterRefresh(ConfigurableOsgiBundleApplicationContext context);

	void refreshFailure(ConfigurableOsgiBundleApplicationContext context, Throwable th);

	void waiting(BootstrappingDependencyEvent event);
}
