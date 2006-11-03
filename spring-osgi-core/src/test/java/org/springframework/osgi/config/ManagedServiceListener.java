package org.springframework.osgi.config;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Hal Hildebrand
 *         Date: Nov 2, 2006
 *         Time: 10:46:51 AM
 */
public class ManagedServiceListener {
    public ArrayList notifications = new ArrayList();


    public void eat(Map props) {
        notifications.add(props);
    }
}
