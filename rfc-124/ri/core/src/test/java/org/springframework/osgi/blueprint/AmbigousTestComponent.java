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

package org.springframework.osgi.blueprint;

/**
 * @author Costin Leau
 */
public class AmbigousTestComponent {

	private String str1, str2;
	private Object obj;


	public AmbigousTestComponent(String arg1, String arg2, Object arg3) {
		this.str1 = arg1;
		this.str2 = arg2;
		this.obj = arg3;
	}

	public String getStr1() {
		return str1;
	}

	public String getStr2() {
		return str2;
	}

	public Object getObj() {
		return obj;
	}
}
