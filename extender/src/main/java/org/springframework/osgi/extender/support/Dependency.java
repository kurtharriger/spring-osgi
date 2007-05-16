package org.springframework.osgi.extender.support;

import org.osgi.framework.*;
import org.springframework.osgi.service.CardinalityOptions;
import org.springframework.osgi.service.importer.OsgiServiceProxyFactoryBean;

/**
 * @author Hal Hildebrand
 */


public class Dependency {
    protected String filterString;
    protected Filter filter;
    protected int cardinality;
    protected BundleContext bundleContext;


    public Dependency(BundleContext bc, OsgiServiceProxyFactoryBean reference) {
        filter = reference.getUnifiedFilter();
        filterString = filter.toString();
        cardinality = reference.getCard();
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
        return !CardinalityOptions.atLeastOneRequired(cardinality) || (refs != null && refs.length != 0);
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