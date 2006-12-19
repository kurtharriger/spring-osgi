/*
 * Copyright 2006 the original author or authors.
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
package org.springframework.osgi.extender.support;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.BundleContext;
import org.springframework.osgi.mock.MockBundle;

public class EntryLookupControllingMockBundle extends MockBundle {
	
	private Enumeration nextFindResult = null;
	private String nextEntryResult = null;
	
	public EntryLookupControllingMockBundle(Dictionary headers) {
		super(headers);
	}

	public void setResultsToReturnOnNextCallToFindEntries(String[] findResult) {
		if (findResult == null) {
			findResult = new String[0];
		}
		this.nextFindResult = createEnumerationOver(findResult);
	}
	
	public Enumeration findEntries(String path, String filePattern, boolean recurse) {
		if (this.nextFindResult == null) {
			return super.findEntries(path, filePattern, recurse);
		}
		else {
			Enumeration ret = this.nextFindResult;
			this.nextFindResult = null;
			return ret;
		}
	}
	
	public void setEntryReturnOnNextCallToGetEntry(String entry) {
		this.nextEntryResult = entry;
	}
	
	public URL getEntry(String name) {
		if (this.nextEntryResult != null) {
			try {
				URL result = new URL(this.nextEntryResult);
				this.nextEntryResult = null;
				return result;
			}
			catch (MalformedURLException ex) {}
			this.nextEntryResult = null;
			return null;
		}
		else {
			return super.getEntry(name);
		}
	}
	
	public URL getResource(String name) {
		return getEntry(name);
	}

	// for OsgiResourceUtils
	public BundleContext getContext() {
		return super.getContext();
	}
	
	private Enumeration createEnumerationOver(String[] entries) {
		return new ArrayEnumerator(entries);
	}
	
	
	private static class ArrayEnumerator implements Enumeration {

		private final String[] source;
		private int index = 0;
		
		public ArrayEnumerator(String[] source) {
			this.source = source;
		}
		
		public boolean hasMoreElements() {
			return source.length > index;
		}

		public Object nextElement() {
			try {
				return new URL(source[index++]);
			}
			catch (MalformedURLException malEx) {
				return null;
			}
		}
		
	}
}