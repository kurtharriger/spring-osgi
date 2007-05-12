package org.springframework.osgi.context.support;

/**
 * @author Hal Hildebrand
 *         Date: May 11, 2007
 *         Time: 10:21:01 PM
 */
public class MissingConfiguration extends Exception{
    public MissingConfiguration(String message) {
        super(message);    
    }
}
