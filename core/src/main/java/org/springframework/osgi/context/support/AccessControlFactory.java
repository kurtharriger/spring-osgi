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
package org.springframework.osgi.context.support;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.DomainCombiner;
import java.security.Permission;
import java.security.ProtectionDomain;

import org.osgi.framework.Bundle;

/**
 * Security utility.
 * 
 * @author Costin Leau
 */
abstract class AccessControlFactory {

	private static class BundleDomainCombiner implements DomainCombiner {

		private final BundleProtectionDomain bpd;

		BundleDomainCombiner(Bundle bundle) {
			bpd = new BundleProtectionDomain(bundle);
		}

		public ProtectionDomain[] combine(ProtectionDomain[] currentDomains, ProtectionDomain[] assignedDomains) {
			return new ProtectionDomain[] { bpd };
		}
	};

	private static class BundleProtectionDomain extends ProtectionDomain {

		private final Bundle bundle;

		BundleProtectionDomain(Bundle bundle) {
			// cannot determine CodeSource or PermissionCollection from a bundle
			super(null, null);
			this.bundle = bundle;
		}

		@Override
		public boolean implies(Permission permission) {
			return bundle.hasPermission(permission);
		}
	}

	/**
	 * Creates an AccessControlContext based on the current security context and the given bundle.
	 * 
	 * @param bundle
	 * @return
	 */
	static AccessControlContext createContext(Bundle bundle) {
		return new AccessControlContext(AccessController.getContext(), new BundleDomainCombiner(bundle));
	}
}
