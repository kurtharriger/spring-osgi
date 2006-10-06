package org.springframework.osgi.test.integration;

import junit.framework.TestCase;
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
        assertEquals(1, result.errorCount());
        assertEquals(2, result.failureCount());
    }
}
