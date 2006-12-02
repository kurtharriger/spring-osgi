package org.springframework.osgi.test.dependencies;

import org.springframework.osgi.test.simpleservice2.MyService2;

/**
 * @author Hal Hildebrand
 *         Date: Dec 1, 2006
 *         Time: 3:39:40 PM
 */
public class DependentImpl implements Dependent {
    private MyService2 service2;
    private MyService2 service3;


    public void setService2(MyService2 service2) {
        this.service2 = service2;
    }


    public void setService3(MyService2 service3) {
        this.service3 = service3;
    }


    public boolean isResolved() {
        return service2 != null && service3 != null;
    }
}
