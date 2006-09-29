package org.springframework.osgi.test.trivial;

import junit.framework.TestCase;

public class TrivialTest extends TestCase {

    private TrivialClass trivial;

    public void setUp() {
	this.trivial = new TrivialClass();
    }

    public void testTrueValue() {
	assertTrue(trivial.trueValue());
    }

    public void testFalseValue() {
	assertFalse(trivial.falseValue());
    }

    public void testIntValue() {
	assertEquals(10,trivial.ten());
    }


}
