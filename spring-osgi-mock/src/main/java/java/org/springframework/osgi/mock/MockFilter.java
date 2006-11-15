/*
 * Copyright 2002-2006 the original author or authors.
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
package org.springframework.osgi.mock;

import java.util.Dictionary;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * @author Costin Leau
 * 
 */
public class MockFilter implements Filter {

	private String filter;

	public MockFilter() {
		this("<no filter>");
	}

	public MockFilter(String filter) {
		this.filter = filter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.Filter#match(org.osgi.framework.ServiceReference)
	 */
	public boolean match(ServiceReference reference) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.Filter#match(java.util.Dictionary)
	 */
	public boolean match(Dictionary dictionary) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.Filter#matchCase(java.util.Dictionary)
	 */
	public boolean matchCase(Dictionary dictionary) {
		return false;
	}

	public String toString() {
		return "filter: " + filter;
	}
}
