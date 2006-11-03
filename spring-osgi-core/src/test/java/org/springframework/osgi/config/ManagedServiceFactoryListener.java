package org.springframework.osgi.config;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Hal Hildebrand
 *         Date: Nov 2, 2006
 *         Time: 10:47:18 AM
 */
public class ManagedServiceFactoryListener {
    public ArrayList notifications = new ArrayList();


    public void eat(String pid, Map props) {
        notifications.add(new Object[]{pid, props});
    }


    public void deleteInstance(String pid) {
        notifications.add(pid);
    }
}
