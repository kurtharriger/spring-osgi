package org.springframework.osgi.context.support;

/**
 * @author Hal Hildebrand
 *         Date: May 11, 2007
 *         Time: 10:21:01 PM
 */
public class MissingConfiguration extends Exception{
    private String missingResource;

    public MissingConfiguration(String message) {
        super(message);    
    }


    public String getMissingResource() {
        return missingResource;
    }


    public void setMissingResource(String missingResource) {
        this.missingResource = missingResource;
    }
}
