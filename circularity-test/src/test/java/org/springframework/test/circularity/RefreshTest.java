package org.springframework.test.circularity;

import junit.framework.TestCase;

/**
 * @author Hal Hildebrand
 *         Date: Apr 13, 2007
 *         Time: 9:16:50 PM
 */
public class RefreshTest extends TestCase {

    public void testRefresh() throws Exception {
        RefreshingContext context = new RefreshingContext(new String[] {"refresh-test.xml"}, getClass());
        // context.refresh();

        assertEquals(1, context.factories.size());
        AFactory aFactory = (AFactory) context.factories.get(0);
        assertTrue(aFactory.isPropertySet());
        assertTrue(aFactory.isInitialized());
        assertTrue(aFactory.isGetObjectCalled()); 
    }
}
