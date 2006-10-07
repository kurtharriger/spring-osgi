package org.springframework.osgi.test.integration;

import java.util.Enumeration;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;

/**
 * @author Hal Hildebrand
 *         Date: Sep 25, 2006
 *         Time: 11:27:24 AM
 */
public class RunBundleCreationTest extends TestCase {
    public void testBundleCreation() throws Exception {
        TestCase test = new BundleCreationTests();
        TestResult result = new TestResult();
        test.setName("testAssertionPass");
        test.run(result);
        test.setName("testAssertionFailure");
        test.run(result);
        test.setName("testFailure");
        test.run(result);
        test.setName("testException");
        test.run(result);

        assertEquals(4, result.runCount());
        
        if (result.errorCount() > 1) {
        	// tell us what went wrong...
        	Enumeration errors = result.errors();
        	while (errors.hasMoreElements()) {
        		TestFailure testFailure = (TestFailure) errors.nextElement();
        		reportOn(testFailure);
        	}
        }
        assertEquals(1, result.errorCount());
        
        if (result.failureCount() > 2) {
        	// tell us what went wrong...
        	Enumeration failures = result.failures();
        	while (failures.hasMoreElements()) {
        		TestFailure testFailure = (TestFailure) failures.nextElement();
        		reportOn(testFailure);
        	}
        }
        assertEquals(2, result.failureCount());
    }
    
    private void reportOn(TestFailure aFailure) {
    	System.err.println(aFailure.failedTest());
    	System.err.println(aFailure.exceptionMessage());
    	System.err.println(aFailure.trace());
    }
}
