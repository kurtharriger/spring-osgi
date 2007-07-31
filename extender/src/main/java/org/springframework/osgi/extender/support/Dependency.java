package org.springframework.osgi.extender.support;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.service.importer.AbstractOsgiServiceProxyFactoryBean;

/**
 * @author Hal Hildebrand
 */


public class Dependency {
    protected String filterString;
    protected Filter filter;
    protected boolean isMandatory;
    protected BundleContext bundleContext;


    public Dependency(BundleContext bc, AbstractOsgiServiceProxyFactoryBean reference) {
        filter = reference.getUnifiedFilter();
        filterString = filter.toString();
        isMandatory = reference.isMandatory();
        bundleContext = bc;
    }


    public boolean matches(ServiceEvent event) {
        return filter.match(event.getServiceReference());
    }


    public void appendTo(StringBuffer sb) {
        sb.append(filterString);
    }


    public boolean isSatisfied() {
        ServiceReference[] refs;
        try {
            refs = bundleContext.getServiceReferences(null, filterString);
        }
        catch (InvalidSyntaxException e) {
            throw (IllegalStateException) new IllegalStateException("Filter '" + filterString
                                                                    + "' has invalid syntax: "
                                                                    + e.getMessage()).initCause(e);
        }
        return !isMandatory || (refs != null && refs.length != 0);
    }


    public String toString() {
        return "Dependency on [" + filter + "]";
    }


    /**
     * @return Returns the filter.
     */
    public Filter getFilter() {
        return filter;
    }

}