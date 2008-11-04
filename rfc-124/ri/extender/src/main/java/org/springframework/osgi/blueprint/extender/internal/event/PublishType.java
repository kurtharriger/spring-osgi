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

package org.springframework.osgi.blueprint.extender.internal.event;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Enum describing the publishing operations supported by the {@link EventAdmin}.
 * Normally this would be implemented by an interface but since the publishing
 * types are predefined, an enum fits nicely.
 * 
 * @author Costin Leau
 * 
 */
enum PublishType {

	SEND {

		@Override
		void publish(EventAdmin admin, Event event) {
			admin.sendEvent(event);
		}
	},

	POST {

		@Override
		void publish(EventAdmin admin, Event event) {
			admin.postEvent(event);
		}
	};

	abstract void publish(EventAdmin admin, Event event);
}
