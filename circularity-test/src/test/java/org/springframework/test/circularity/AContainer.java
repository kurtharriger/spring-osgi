package org.springframework.test.circularity;

/**
 * @author Hal Hildebrand
 *         Date: Apr 13, 2007
 *         Time: 10:08:28 PM
 */
public class AContainer {
    private Object object;


    public Object getObject() {
        return object;
    }


    public void setObject(Object object) {
        this.object = object;
    }
}
