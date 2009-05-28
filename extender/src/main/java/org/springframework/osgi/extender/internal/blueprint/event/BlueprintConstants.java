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

import org.osgi.service.event.EventConstants;

/**
 * Utility interface aggregating the event properties from various OSGi APIs in one single place.
 * 
 * @author Costin Leau
 */
interface BlueprintConstants {

	static final String BUNDLE = "bundle";
	static final String BUNDLE_ID = "bundle.id";
	static final String BUNDLE_NAME = "bundle.name";
	static final String BUNDLE_SYM_NAME = EventConstants.BUNDLE_SYMBOLICNAME;
	static final String BUNDLE_VERSION = "bundle.version";
	static final String TIMESTAMP = EventConstants.TIMESTAMP;

	static final String EVENT = EventConstants.EVENT;

	static final String EXTENDER_BUNDLE = org.osgi.service.blueprint.container.EventConstants.EXTENDER_BUNDLE;
	static final String EXTENDER_BUNDLE_ID = org.osgi.service.blueprint.container.EventConstants.EXTENDER_BUNDLE_ID;
	static final String EXTENDER_BUNDLE_SYM_NAME = org.osgi.service.blueprint.container.EventConstants.EXTENDER_BUNDLE_SYMBOLICNAME;
	static final String EXTENDER_BUNDLE_VERSION = org.osgi.service.blueprint.container.EventConstants.EXTENDER_BUNDLE_VERSION;

	static final String EXCEPTION = EventConstants.EXCEPTION;
	static final String EXCEPTION_CLASS = EventConstants.EXECPTION_CLASS;
	static final String EXCEPTION_MESSAGE = EventConstants.EXCEPTION_MESSAGE;

	static final String SERVICE_OBJECTCLASS = EventConstants.SERVICE_OBJECTCLASS;
	static final String SERVICE_FILTER = "service.filter";

	static final String TOPIC_BLUEPRINT_EVENTS = "org/osgi/service/blueprint";
	static final String TOPIC_CREATING = TOPIC_BLUEPRINT_EVENTS + "/container/CREATING";
	static final String TOPIC_CREATED = TOPIC_BLUEPRINT_EVENTS + "/container/CREATED";
	static final String TOPIC_DESTROYING = TOPIC_BLUEPRINT_EVENTS + "/container/DESTROYING";
	static final String TOPIC_DESTROYED = TOPIC_BLUEPRINT_EVENTS + "/container/DESTROYED";
	static final String TOPIC_WAITING = TOPIC_BLUEPRINT_EVENTS + "/container/WAITING";
	static final String TOPIC_FAILURE = TOPIC_BLUEPRINT_EVENTS + "/container/FAILURE";

	static final String EVENT_FILTER = EventConstants.EVENT_FILTER;
}
