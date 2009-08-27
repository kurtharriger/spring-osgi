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

import java.util.Collection;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.springframework.osgi.blueprint.config.RegionCode;

/**
 * Just a simple component used by the namespace tests.
 * 
 * @author Costin Leau
 * 
 */
public class TestComponent implements java.io.Serializable {

	private Object propA;
	private Object propB;
	private ServiceReference serviceReference;

	public TestComponent() {
	}

	public TestComponent(Object arg) {
		propA = arg;
	}

	public TestComponent(int arg) {
		propA = arg;
	}
	
	public TestComponent(String str) {
		this.propA = str;
	}
	
	
	public TestComponent(RegionCode str) {
		this.propA = str;
	}
	
	public TestComponent(Double dbl) {
		this.propA = dbl;
	}

	public TestComponent(Object arg1, Object arg2) {
		propA = arg1;
		propB = arg2;
	}

	public Object getPropA() {
		return propA;
	}

	public void setPropA(Object property) {
		this.propA = property;
	}

	public Object getPropB() {
		return propB;
	}

	public void setPropB(Object propB) {
		this.propB = propB;
	}

	public void setList(List list) {
		this.propA = list;
	}

	public void setCollection(Collection col) {
		this.propA = col;
	}

	public void setArray(RegionCode[] reg) {
		this.propA = reg;
	}
	
	public ServiceReference getServiceReference() {
		return serviceReference;
	}

	public void setServiceReference(ServiceReference serviceReference) {
		this.serviceReference = serviceReference;
	}
	
	public void setBool(boolean bool) {
		this.propA = Boolean.valueOf(bool);
	}

	public void init() {
		System.out.println("Initialized");
	}
}