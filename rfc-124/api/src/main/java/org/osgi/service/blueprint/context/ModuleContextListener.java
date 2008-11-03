
package org.osgi.service.blueprint.context;

import org.osgi.framework.Version;

public interface ModuleContextListener extends java.util.EventListener {

	void contextCreated(String bundleSymbolicName, Version version);

	void contextCreationFailed(String bundleSymbolicName, Version version, Throwable rootCause);
}
