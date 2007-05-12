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

import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.springframework.osgi.util.ConfigUtils;
import org.springframework.osgi.util.OsgiBundleUtils;
import org.springframework.util.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Determine configuration information needed to construct an application
 * context for a given bundle
 * 
 * @author Adrian Colyer
 * @author Costin Leau
 */
public class ApplicationContextConfiguration {

	private final Bundle bundle;

	private boolean asyncCreation = ConfigUtils.DIRECTIVE_CREATE_ASYNCHRONOUSLY_DEFAULT;

	private String[] configurationLocations = new String[0];

	private boolean isSpringPoweredBundle = true;

	private boolean publishContextAsService = ConfigUtils.DIRECTIVE_PUBLISH_CONTEXT_DEFAULT;

	private String toString;

    private long timeout = ConfigUtils.DIRECTIVE_TIMEOUT_DEFAULT;

    private static final Log log = LogFactory.getLog(ApplicationContextConfiguration.class);


    public ApplicationContextConfiguration(Bundle forBundle) {
		this.bundle = forBundle;
		initialise();
		// create toString
		StringBuffer buf = new StringBuffer();
		buf.append("AppCtxCfg [Bundle=");
		buf.append(OsgiBundleUtils.getNullSafeSymbolicName(bundle));
		buf.append("]isSpringBundle=");
		buf.append(isSpringPoweredBundle);
		buf.append("|async=");
		buf.append(asyncCreation);
		buf.append("|publishCtx=");
		buf.append(publishContextAsService);
		buf.append("|timeout=");
		buf.append(timeout);
		toString = buf.toString();
        if (log.isDebugEnabled()) {
            log.debug("configuration: " + toString);
        }
    }

	/**
	 * True if this bundle has at least one defined application context
	 * configuration file.
	 * 
	 * A bundle is "Spring-Powered" if it has at least one configuration file.
	 */
	public boolean isSpringPoweredBundle() {
		return this.isSpringPoweredBundle;
	}

	/**
	 * How long should the application context wait for dependent services
     * to be satisfied on context creation?
	 */
	public long getTimeout() {
		return this.timeout;
	}

	/**
	 * Should the application context wait for all non-optional service
	 * references to be satisfied before starting?
	 */
	public boolean isCreateAsynchronously() {
		return this.asyncCreation;
	}

	/**
	 * @return Returns the publishContextAsService.
	 */
	public boolean isPublishContextAsService() {
		return publishContextAsService;
	}

	/**
	 * The locations of the configuration files used to build the application
	 * context (as Spring resource paths).
	 * @return
	 */
	public String[] getConfigurationLocations() {
		return this.configurationLocations;
	}

	/**
	 * 
	 * If the Spring-Context header is present the resource files defined there
	 * will be used, otherwise all xml files in META-INF/spring will be treated
	 * as application context configuration files.
	 */
	private void initialise() {
		Dictionary headers = bundle.getHeaders();

		this.isSpringPoweredBundle = ConfigUtils.isSpringOsgiPoweredBundle(bundle);

		if (isSpringPoweredBundle) {
			String springContextHeader = ConfigUtils.getSpringContextHeader(headers);
			if (StringUtils.hasText(springContextHeader)) {
				this.timeout = ConfigUtils.getTimeOut(headers);
				this.publishContextAsService = ConfigUtils.getPublishContext(headers);
				this.asyncCreation = ConfigUtils.getCreateAsync(headers);
                try {
                    this.configurationLocations = ConfigUtils.getConfigLocations(headers, bundle);
                } catch (MissingConfiguration e) {
                    this.configurationLocations = new String[0];
                    this.isSpringPoweredBundle = false;
                }
                if (this.configurationLocations.length == 0) {
                    this.isSpringPoweredBundle = false;
                }
            }
        }
    }

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return toString;
	}

}
