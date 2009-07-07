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
package org.springframework.osgi.iandt.blueprint.core;

import java.awt.Shape;
import java.awt.geom.Area;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Costin Leau
 */
public class InitBean {

	private List list;
	private BundleContext bundleContext;

	public void init() {
		System.out.println("List size is " + list.size());
		System.out.println(list.size());
		ServiceRegistration reg = bundleContext.registerService(Shape.class.getName(), new Area(), null);
		System.out.println(list.size());
		reg.unregister();
		System.out.println(list.size());
	}

	public void setList(List list) {
		this.list = list;
	}

	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}
}
