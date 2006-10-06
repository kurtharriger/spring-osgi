package org.springframework.osgi.test;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

public class MavenArtifactFinderTest extends TestCase {

	public void testFindMyArtifact()throws IOException {
		MavenPackagedArtifactFinder finder = 
			new MavenPackagedArtifactFinder("test-artifact","1.0-SNAPSHOT");
		File found = finder.findPackagedArtifact(new File("target/test-classes/org/springframework/osgi/test"));
		assertNotNull(found);
		assertTrue(found.exists());
	}

	public void testFindChildArtifact()throws IOException {
		MavenPackagedArtifactFinder finder = 
			new MavenPackagedArtifactFinder("test-child-artifact","1.0-SNAPSHOT");
		File found = finder.findPackagedArtifact(new File("target/test-classes/org/springframework/osgi/test"));
		assertNotNull(found);
		assertTrue(found.exists());
	}

	public void testFindParentArtifact()throws IOException {
		MavenPackagedArtifactFinder finder = 
			new MavenPackagedArtifactFinder("test-artifact","1.0-SNAPSHOT");
		File found = finder.findPackagedArtifact(new File("target/test-classes/org/springframework/osgi/test/child"));
		assertNotNull(found);
		assertTrue(found.exists());
	}

}
