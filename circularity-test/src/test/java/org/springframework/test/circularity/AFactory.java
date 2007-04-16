package org.springframework.test.circularity;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Hal Hildebrand
 *         Date: Apr 13, 2007
 *         Time: 9:17:26 PM
 */
public class AFactory implements FactoryBean, InitializingBean {

    private boolean initialized = false;
    private Object listener;
    private boolean getObjectCalled = false;


    public boolean isInitialized() {
        return initialized;
    }


    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }


    public boolean isPropertySet() {
        return listener != null;
    }


    public boolean isGetObjectCalled() {
        return getObjectCalled;
    } 

    public void afterPropertiesSet() throws Exception {
        initialized = true;
    }


    public Object getListener() {
        return listener;
    }


    public void setListener(Object listener) {
        this.listener = listener;
    }


    public Object getObject() throws BeansException {
        if (!initialized) {
            throw new IllegalStateException("aPS has not been called");
        }
        if (!isPropertySet()) {
            throw new IllegalStateException("property has not been set");
        }
        getObjectCalled = true;
        return new Object();
    }


    public Class getObjectType() {
        return Object.class;
    }


    public boolean isSingleton() {
        return true;
    }
}
