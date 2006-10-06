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
package org.springframework.osgi.test.integration;

import junit.framework.TestCase;
import junit.framework.TestResult;

import org.springframework.osgi.test.integration.basic.BasicTests;

/**
 * Run the actual test. This test fires BasicTests to check if the testing
 * framework for OSGi works properly. BasicTests contains some tests which fail
 * on purpose which will be asserted by the current test.
 *
 * @author Costin Leau
 */
public class RunBasicTest extends TestCase {

    public void testBasicTests() throws Exception {
        TestCase test = new BasicTests();
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
