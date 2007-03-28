/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.springframework.osgi.context.support;

import org.springframework.context.ApplicationEvent;
import org.springframework.osgi.util.OsgiServiceUtils;
import org.osgi.framework.Bundle;

/**
 * @author Andy Piper
 * @since 2.1
 */
public class SpringBundleEvent extends ApplicationEvent {
	private int eventType;

	public SpringBundleEvent(int type, Bundle bundle) {
		super(bundle);
		eventType = type;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final SpringBundleEvent event = (SpringBundleEvent) o;

		if (getType() != event.getType()) return false;
		if (!getSource().equals(event.getSource())) return false;

		return true;
	}

	public int hashCode() {
		return getType();
	}

	public int getType() {
		return eventType;
	}

	public String toString() {
		 return "[" + OsgiServiceUtils.getBundleEventAsString(eventType)
			 + ", " + ((Bundle)getSource()).getSymbolicName() + "]";
	}

}
