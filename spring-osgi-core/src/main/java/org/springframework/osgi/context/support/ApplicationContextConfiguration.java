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
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.springframework.osgi.io.OsgiBundleResource;
import org.springframework.util.StringUtils;

/**
 * Determine configuration information needed to construct an
 * application context for a given bundle
 *
 * @author Adrian Colyer
 */
public class ApplicationContextConfiguration {

    private static final String CONTEXT_LOCATION_HEADER = "Spring-Context";
    private static final String CONTEXT_LOCATION_DELIMITERS = ", ";
    private static final String DONT_WAIT_FOR_DEPENDENCIES_DIRECTIVE = ";wait-for-dependencies:=false";
	private static final String CREATE_ASYNCHRONOUSLY_DIRECTIVE = ";create-asynchronously:=false";
    private static final String SPRING_CONTEXT_DIRECTORY = "/META-INF/spring/";

    private static final Log log = LogFactory.getLog(ApplicationContextConfiguration.class);

    private final Bundle bundle;
    private boolean waitForDependencies = true;
	private boolean asyncCreation = true;
    private String[] configurationLocations = null;
    private boolean isSpringPoweredBundle = true;

    public ApplicationContextConfiguration(Bundle forBundle) {
        this.bundle = forBundle;
        initialise();
    }

    /**
     * True if this bundle has at least one defined application context
     * configuration file.
     */
    public boolean isSpringPoweredBundle() {
        return this.isSpringPoweredBundle;
    }

    /**
     * Should the application context wait for all non-optional service
     * references to be satisfied before starting?
     */
    public boolean waitForDependencies() {
        return this.waitForDependencies;
    }

	/**
     * Should the application context wait for all non-optional service
     * references to be satisfied before starting?
     */
    public boolean createAsynchronously() {
        return this.asyncCreation;
    }

    /**
     * The locations of the configuration files used to build the
     * application context (as Spring resource paths).
     * @return
     */
    public String[] getConfigurationLocations() {
        return this.configurationLocations;
    }

    /**
     * A bundle is "Spring-Powered" if it has at least one configuration file.
     * If the Spring-Context header is present the resource files defined there will
     * be used, otherwise all xml files in META-INF/spring will be treated as application
     * context configuration files.
     *
     * Unless the Spring-Context manifest entry contains the directive "wait-for-dependencies:=false"
     * then the app context should wait for all service references to be satisfied before starting.
     */
    private void initialise() {
        Dictionary manifestHeaders = bundle.getHeaders();
        String springContextHeader = (String) manifestHeaders.get(CONTEXT_LOCATION_HEADER);
        if (springContextHeader == null) {
            this.configurationLocations = findSpringResourcesInMetaInf();
        }
        else {
	        if (springContextHeader.indexOf(CREATE_ASYNCHRONOUSLY_DIRECTIVE) != -1) {
	            this.asyncCreation = false;
		        this.waitForDependencies = false;
	        }
            else if (springContextHeader.indexOf(DONT_WAIT_FOR_DEPENDENCIES_DIRECTIVE) != -1) {
                this.waitForDependencies = false;
            }
            populateConfigLocationsFromContextHeader(springContextHeader);
        }

        if (this.configurationLocations == null) {
            this.isSpringPoweredBundle = false;
        }

    }

    /**
     * Find all of the xml resources in META-INF/spring and return an array of
     * Spring resource paths pointing to them.
     */
    private String[] findSpringResourcesInMetaInf() {
        List resourceList = new ArrayList();
        Enumeration resources = bundle.findEntries(SPRING_CONTEXT_DIRECTORY, "*.xml", false /* don't recurse */);
        if (resources != null) {
            while (resources.hasMoreElements()) {
                URL resourceURL = (URL) resources.nextElement();
                resourceList.add(OsgiBundleResource.BUNDLE_URL_URL_PREFIX + resourceURL.toExternalForm());
            }
        }
        if (resourceList.isEmpty()) {
            return null;
        } else {
            String[] ret = new String[resourceList.size()];
            return (String[]) resourceList.toArray(ret);
        }
    }

    /**
     * The Spring-Context header contains a comma-delimited set of context entries, each entry is
     * a path to a configuration file, optionally followed by a directive.
     * The special path "*" means include all resources in META-INF/spring
     * @param springContextHeader
     */
    private void populateConfigLocationsFromContextHeader(String springContextHeader) {
        boolean addMetaInfResources = false;
        String[] contextEntries = StringUtils.tokenizeToStringArray(springContextHeader, CONTEXT_LOCATION_DELIMITERS);
        List configLocationPaths = new ArrayList();
        for (int i = 0; i < contextEntries.length; i++) {
            String path = stripDirectives(contextEntries[i]);
            if ("*".equals(path)) {
                addMetaInfResources = true;
            }
            else {
                if (bundle.getEntry(path) != null) {
                    configLocationPaths.add(OsgiBundleResource.BUNDLE_URL_PREFIX  + path);
                }
                else {
                    if (log.isWarnEnabled()) {
                        log.warn("Spring-Context manifest entry for bundle '" + this.bundle.getSymbolicName() +
                                "' contained path '" + path + "' but no corresponding " +
                                "resource was found in the bundle, ignoring");
                    }
                }
            }
        }

        // now add in meta-inf resources if needed
        if (addMetaInfResources) {
            String[] metaInfResources = findSpringResourcesInMetaInf();
            if (metaInfResources != null) {
                for (int r = 0; r < metaInfResources.length; r++) {
                    configLocationPaths.add(metaInfResources[r]);
                }
            }
        }

        // populate the configurationLocations based on the result
        if (configLocationPaths.isEmpty()) {
            this.configurationLocations = null;
        } else {
            this.configurationLocations = (String[])
                configLocationPaths.toArray(new String[configLocationPaths.size()]);
        }

    }

    /**
     * Keep everything before the directive delimiter (';')
     */
    private String stripDirectives(String contextHeaderEntry) {
        if (contextHeaderEntry.indexOf(';') != -1) {
            return contextHeaderEntry.substring(0, contextHeaderEntry.indexOf(';'));
        }
        else {
            return contextHeaderEntry;
        }
    }

}
