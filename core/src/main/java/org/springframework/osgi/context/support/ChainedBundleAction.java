/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.osgi.context.support;

import org.osgi.framework.Bundle;
import org.springframework.util.ObjectUtils;

/**
 * Delegate class that allows assembling various {@link BundleAction} for
 * serialized execution. Useful for creating chains of related actions such as:
 * install -> start -> update or stop-> uninstall.
 * 
 * 
 * @author Costin Leau
 * 
 */
public class ChainedBundleAction implements BundleAction {

	private final BundleAction[] actions;

	ChainedBundleAction(BundleAction[] actions) {
		this.actions = (ObjectUtils.isEmpty(actions) ? new BundleAction[0] : actions);
	}

	public Bundle execute(Bundle bundle) {
		Bundle bnd = bundle;
		for (int i = 0; i < actions.length; i++) {
			bnd = actions[i].execute(bnd);
		}

		return bnd;
	}

}
