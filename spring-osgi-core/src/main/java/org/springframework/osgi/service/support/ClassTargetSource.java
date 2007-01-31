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
package org.springframework.osgi.service.support;

import org.springframework.aop.TargetSource;

/**
 * Empty TargetSource implementation for class based proxies.
 * (will be removed once Spring 2.0.3 is out)
 * 
 * @author Costin Leau
 * 
 */
public class ClassTargetSource implements TargetSource {

	private final Class clazz;

	public ClassTargetSource(Class clazz) {
		this.clazz = clazz;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.aop.TargetSource#getTarget()
	 */
	public Object getTarget() throws Exception {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.aop.TargetSource#getTargetClass()
	 */
	public Class getTargetClass() {
		return clazz;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.aop.TargetSource#isStatic()
	 */
	public boolean isStatic() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.aop.TargetSource#releaseTarget(java.lang.Object)
	 */
	public void releaseTarget(Object target) throws Exception {
	}

}
