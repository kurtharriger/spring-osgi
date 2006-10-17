package org.springframework.osgi.test.othertests;

import org.springframework.osgi.test.ConfigurableBundleCreatorTests;

/**
 * @author Hal Hildebrand Date: Oct 14, 2006 Time: 10:12:06 AM
 */
public class StreamCorruptionTest extends ConfigurableBundleCreatorTests {
	public StreamCorruptionTest() {
		System.setProperty(OSGI_FRAMEWORK_SELECTOR, EQUINOX_PLATFORM);
	}

	protected String getManifestLocation() {
		return "classpath:org/springframework/osgi/test/othertests/StreamCorruptionTest.MF";
	}

	protected String[] getBundleLocations() {
		return new String[] { localMavenBuildArtifact("aopalliance.osgi", "1.0-SNAPSHOT"),
				localMavenBuildArtifact("commons-collections.osgi", "3.2-SNAPSHOT"),
				localMavenBuildArtifact("spring-aop", "2.1-SNAPSHOT"),
				localMavenBuildArtifact("spring-context", "2.1-SNAPSHOT"),
				localMavenBuildArtifact("spring-beans", "2.1-SNAPSHOT"), };
	}

	public void testMe() throws Exception {
		getBundleContext().installBundle("file:///foo/bar/baz.jar");
	}
}
