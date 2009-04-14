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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.framework.ServiceRegistration;

/**
 * @author Costin Leau
 */
public class ExporterListener {

	public static final List bind = new ArrayList();
	public static final List unbind = new ArrayList();


	public void up(ServiceRegistration reg, Map serviceProperties) {
		bind.add(reg);
	}

	public void down(ServiceRegistration reg, Map serviceProperties) {
		unbind.add(reg);
	}
}