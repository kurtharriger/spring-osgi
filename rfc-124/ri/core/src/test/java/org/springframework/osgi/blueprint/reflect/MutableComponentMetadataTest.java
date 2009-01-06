
package org.springframework.osgi.blueprint.reflect;

import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

public class MutableComponentMetadataTest {

	@Test(expected = IllegalArgumentException.class)
	public void testNullName() {
		new MCM(null);
	}

	@Test
	public void testName() {
		MCM m = new MCM("foo");
		assertEquals("foo", m.getName());
	}

	@Test
	public void testSetName() {
		MCM m = new MCM("foo");
		m.setName("bar");
		assertEquals("bar", m.getName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetNullName() {
		MCM m = new MCM("foo");
		m.setName(null);
	}

	@Test
	public void testInitialAliases() {
		MCM m = new MCM("foo");
		assertEquals("no aliases", 0, m.getAliases().length);
	}

	@Test
	public void testAddAlias() {
		MCM m = new MCM("foo");
		m.addAlias("alias");
		assertEquals("one alias", 1, m.getAliases().length);
		assertEquals("alias", m.getAliases()[0]);
		m.addAlias("anotherone");
		assertEquals("two aliases", 2, m.getAliases().length);
		assertEquals("anotherone", m.getAliases()[1]);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddNullAlias() {
		MCM m = new MCM("foo");
		m.addAlias(null);
	}

	@Test
	public void testRemoveAlias() {
		MCM m = new MCM("foo");
		m.addAlias("alias");
		m.removeAlias("alias");
		assertEquals("no aliases", 0, m.getAliases().length);
		m.removeAlias("notthere");
		assertEquals("no aliases", 0, m.getAliases().length);
		m.addAlias("alias");
		m.addAlias("anotherone");
		m.removeAlias("alias");
		assertEquals("one alias", 1, m.getAliases().length);
		assertEquals("anotherone", m.getAliases()[0]);
	}

	@Test
	public void testRemoveNullAlias() {
		MCM m = new MCM("foo");
		m.removeAlias(null);
	}

	@Test
	public void testInitialDependencies() {
		MCM m = new MCM("foo");
		assertEquals("no aliases", 0, m.getAliases().length);
	}

	@Test
	public void testAddHependency() {
		MCM m = new MCM("foo");
		m.addDependency("dependency");
		assertEquals("one dependency", 1, m.getExplicitDependencies().size());
		assertEquals("dependency", m.getExplicitDependencies().iterator().next());
		m.addDependency("anotherone");
		assertEquals("two dependencies", 2, m.getExplicitDependencies().size());
		Iterator iter = m.getExplicitDependencies().iterator();
		iter.next();
		assertEquals("anotherone", iter.next());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddNullDependency() {
		MCM m = new MCM("foo");
		m.addDependency(null);
	}

	@Test
	public void testRemoveDependency() {
		MCM m = new MCM("foo");
		m.addDependency("dependency");
		m.removeDependency("dependency");
		assertEquals("no dependencies", 0, m.getExplicitDependencies().size());
		m.removeDependency("notthere");
		assertEquals("no dependencies", 0, m.getExplicitDependencies().size());
		m.addDependency("dependency");
		m.addDependency("anotherone");
		m.removeDependency("dependency");
		assertEquals("one dependency", 1, m.getExplicitDependencies().size());
		assertEquals("anotherone", m.getExplicitDependencies().iterator().next());
	}

	@Test
	public void testRemoveNullDependency() {
		MCM m = new MCM("foo");
		m.removeDependency(null);
	}


	// class under test is abstract, so this makes it easier to test
	private static class MCM extends MutableComponentMetadata {

		public MCM(String name) {
			super(name);
		}
	}
}
