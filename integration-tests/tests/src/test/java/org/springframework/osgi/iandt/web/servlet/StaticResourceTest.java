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

package org.springframework.osgi.iandt.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.osgi.iandt.web.BaseWebIntegrationTest;
import org.springframework.osgi.iandt.web.HttpClient;

/**
 * Integration test for the simple servlet bundle.
 * 
 * @author Costin Leau
 * 
 */
public class StaticResourceTest extends BaseWebIntegrationTest {

	/** logger */
	private static final Log log = LogFactory.getLog(StaticResourceTest.class);

	private String BASE;

	private final String GROUP_ID = "resources.only";


	protected void onSetUp() throws Exception {
		BASE = GROUP_ID + "-" + getSpringDMVersion();
	}

	protected String[] getTestBundlesNames() {
		return new String[] { WEB_TESTS_GROUP + "," + GROUP_ID + "," + getSpringDMVersion() };
	}

	public void testWarIndexPage() throws Exception {
		assertTrue(HttpClient.getLocalResponse(BASE, "index.html").isOk());
	}

	public void testIndexRedirect() throws Exception {
		assertTrue(HttpClient.getLocalResponse(BASE, "").isOk());
	}

	public void testOtherPage() throws Exception {
		assertTrue(HttpClient.getLocalResponse(BASE, "other.html").isOk());
	}

	public void testNestedPage() throws Exception {
		assertTrue(HttpClient.getLocalResponse(BASE, "nested/page.html").isOk());
	}

	public void testUnexistingNestedPage() throws Exception {
		assertTrue(HttpClient.getLocalResponse(BASE, "nested/no-such-page.html").isNotFound());
	}

}
