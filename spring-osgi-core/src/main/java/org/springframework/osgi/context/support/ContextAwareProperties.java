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
package org.springframework.osgi.context.support;

import java.util.HashMap;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.BeansException;
import org.osgi.framework.BundleContext;

/**
 * LocalBundleContext indexed properties.
 * 
 * @author Andy Piper
 */
public class ContextAwareProperties extends Properties implements InitializingBean, BeanPostProcessor
{

	private static final long serialVersionUID = 4219438743834974209L;

	private HashMap contextToPropMap = new HashMap();

  public ContextAwareProperties() {
    this(System.getProperties());
  }

	public ContextAwareProperties(Properties props) {
		super(props);
	}

	public synchronized Object setProperty(String key, String value) {
    if (key == null || value == null) throw new NullPointerException();
    BundleContext cl = LocalBundleContext.getContext();
    if (cl != null) {
  		Properties p = (Properties)contextToPropMap.get(cl);
  		if (p == null) {
  			p = new Properties();
  			contextToPropMap.put(cl, p);
  		}
  		return p.setProperty(key, value);
    }
    return super.setProperty(key, value);
	}

	public synchronized String getProperty(String key) {
    if (key == null) throw new NullPointerException();
    BundleContext cl = LocalBundleContext.getContext();
    if (cl != null) {
      Properties p = (Properties)contextToPropMap.get(cl);
		  if (p != null) {
		    String prop = (String)p.get(key);
        if (prop != null) return prop;
      }
    }
    return super.getProperty(key);
	}

  public void afterPropertiesSet() throws Exception {
    System.setProperties(this);
  }

  public Object postProcessBeforeInitialization(Object o, String s) throws BeansException {
    return o;
  }

  public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
    return o;
  }
}
