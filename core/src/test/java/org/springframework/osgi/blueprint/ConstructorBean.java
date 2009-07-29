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

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Costin Leau
 */
public class ConstructorBean {

	private Object value;

	public ConstructorBean(URL url) {
		this.value = url;
	}

	public ConstructorBean(URL[] url) {
		this.value = url;
	}

	public ConstructorBean(List[] lists) {
		this.value = lists;
	}

	public ConstructorBean(Map[] lists) {
		this.value = lists;
	}

	public ConstructorBean(String[] lists) {
		this.value = lists;
	}

	public ConstructorBean(char[] lists) {
		this.value = lists;
	}

	public ConstructorBean(String[][] lists) {
		this.value = lists;
	}

	
	public ConstructorBean() {
	}

	public ConstructorBean(boolean bool) {
		value = bool;
	}

	public ConstructorBean(Boolean bool) {
		value = bool;
	}

	public Object getValue() {
		return value;
	}

	public Object makeInstance(boolean bool) {
		return Boolean.valueOf(bool);
	}

	public Object makeInstance(Boolean bool) {
		return bool;
	}

	public Object makeInstance(URL url) {
		return url;
	}

	public Object makeInstance(String str) {
		return str;
	}

	public Object makeInstance(Class arg2) {
		return arg2;
	}

	public Object makeInstance(File arg2) {
		return arg2;
	}

	public Object makeInstance(Locale arg2) {
		return arg2;
	}

	public Object makeInstance(Date arg2) {
		return arg2;
	}

	public Object makeInstance(Map arg2) {
		return arg2;
	}

	public Object makeInstance(Set arg2) {
		return arg2;
	}

	public Object makeInstance(List arg2) {
		return arg2;
	}

	public Object makeInstance(Date[] arg1) {
		return arg1;
	}

	public Object makeInstance(URL[] arg1) {
		return arg1;
	}

	public Object makeInstance(Class[] arg1) {
		return arg1;
	}

	public Object makeInstance(Locale[] arg1) {
		return arg1;
	}

	public Object makeInstance(List[] arg1) {
		return arg1;
	}

	public Object makeInstance(Set[] arg1) {
		return arg1;
	}

//	public Object makeInstance(Map[] arg1) {
//		return arg1;
//	}
}