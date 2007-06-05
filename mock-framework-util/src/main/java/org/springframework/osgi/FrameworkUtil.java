package org.springframework.osgi;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author Hal Hildebrand
 *         Date: Jun 5, 2007
 *         Time: 1:09:47 AM
 */
public class FrameworkUtil {

    public static Filter createFilter(String filter) throws InvalidSyntaxException {
        return new MockFilter(filter);
    }
}
