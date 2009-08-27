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

package org.springframework.osgi.extender.internal.blueprint.activator.support;

import java.util.Dictionary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.springframework.osgi.extender.support.ApplicationContextConfiguration;
import org.springframework.osgi.extender.support.internal.ConfigUtils;
import org.springframework.osgi.util.OsgiStringUtils;

/**
 * Extension to the default {@link ApplicationContextConfiguration} that overrides Spring DM settings with RFC 124.
 * 
 * @author Costin Leau
 * 
 */
public class BlueprintContainerConfig extends ApplicationContextConfiguration {

	/** logger */
	private static final Log log = LogFactory.getLog(BlueprintContainerConfig.class);

	private final long timeout;
	private final boolean createAsync;
	private final boolean waitForDep;
	private final boolean publishContext;
	private final boolean hasTimeout;
	private final String toString;

	public BlueprintContainerConfig(Bundle bundle) {
		super(bundle, new BlueprintConfigurationScanner());

		Dictionary headers = bundle.getHeaders();

		hasTimeout = BlueprintConfigUtils.hasTimeout(headers);
		long option = BlueprintConfigUtils.getTimeOut(headers);
		// no need to translate into ms
		timeout = (option >= 0 ? option : ConfigUtils.DIRECTIVE_TIMEOUT_DEFAULT * 1000);
		createAsync = BlueprintConfigUtils.getCreateAsync(headers);
		waitForDep = BlueprintConfigUtils.getWaitForDependencies(headers);
		publishContext = BlueprintConfigUtils.getPublishContext(headers);

		StringBuilder buf = new StringBuilder();
		buf.append("Blueprint Config [Bundle=");
		buf.append(OsgiStringUtils.nullSafeSymbolicName(bundle));
		buf.append("]isBlueprintBundle=");
		buf.append(isSpringPoweredBundle());
		buf.append("|async=");
		buf.append(createAsync);
		buf.append("|graceperiod=");
		buf.append(waitForDep);
		buf.append("|publishCtx=");
		buf.append(publishContext);
		buf.append("|timeout=");
		buf.append(timeout);
		buf.append("ms");
		toString = buf.toString();

		if (log.isTraceEnabled()) {
			log.trace("Configuration: " + toString);
		}
	}

	@Override
	public boolean isTimeoutDeclared() {
		return hasTimeout;
	}

	public long getTimeout() {
		return timeout;
	}

	public boolean isCreateAsynchronously() {
		return createAsync;
	}

	public boolean isWaitForDependencies() {
		return waitForDep;
	}

	public boolean isPublishContextAsService() {
		return publishContext;
	}

	public String toString() {
		return toString;
	}
}