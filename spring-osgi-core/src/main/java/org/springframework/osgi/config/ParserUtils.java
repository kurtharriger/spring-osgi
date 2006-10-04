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
 *
 */
package org.springframework.osgi.config;

import java.util.StringTokenizer;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.util.StringUtils;
import org.w3c.dom.Attr;

/**
 * @author Andy Piper
 * @since 2.1
 */
public class ParserUtils
{
	public final static String LISTENER_ID = "listeners";
	public final static String DEPENDS_ON = "depends-on";
  public static final String LAZY_INIT = "lazy-init";

	public static void parseDependsOn(Attr attribute, BeanDefinitionBuilder builder) {
		for (StringTokenizer dependents = new StringTokenizer(attribute.getValue(), ", ");
		     dependents.hasMoreElements();) {
			String dep = (String) dependents.nextElement();
			if (StringUtils.hasText(dep)) {
				builder.addDependsOn(dep);
			}
		}
	}

	public static void parseListeners(Attr attribute, BeanDefinitionBuilder builder) {
		ManagedList alist = new ManagedList();
		for (StringTokenizer listeners = new StringTokenizer(attribute.getValue(), ", ");
		     listeners.hasMoreElements();) {
			String l = (String) listeners.nextElement();
			if (StringUtils.hasText(l)) {
				alist.add(new RuntimeBeanReference(l));
			}
		}
		builder.addPropertyValue(LISTENER_ID, alist);
	}

}
