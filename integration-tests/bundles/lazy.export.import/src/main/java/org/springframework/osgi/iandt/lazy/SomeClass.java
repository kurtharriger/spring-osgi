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

package org.springframework.osgi.iandt.lazy;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.iandt.lazy.companion.PublicClass;
import org.springframework.osgi.iandt.lazy.internal.InternalClass;

/**
 * Dummy class used for checking lazy activation.
 * 
 * @author Costin Leau
 */
public class SomeClass extends PublicClass implements ServiceListener, InitializingBean {

    
    private static final BeanCreationException ex = new BeanCreationException("test");

    public SomeClass() {
        super();
    }
    
	public void serviceChanged(ServiceEvent event) {
	}

	public void afterPropertiesSet() {
	}

	private InternalClass someInternalClass() {
		return null;
	}
}
