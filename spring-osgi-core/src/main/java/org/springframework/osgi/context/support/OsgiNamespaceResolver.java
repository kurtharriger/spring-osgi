/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.springframework.osgi.context.support;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * @author Hal Hildebrand
 *         Date: May 24, 2006
 *         Time: 12:55:10 PM
 */
public class OsgiNamespaceResolver implements NamespaceHandlerResolver, EntityResolver {
    private final ArrayList namespaceHandlerResolvers = new ArrayList();
    private final ArrayList entityResolvers = new ArrayList();
    private final BundleContext osgiBundleContext;


    public OsgiNamespaceResolver(BundleContext osgiBundleContext) {
        this.osgiBundleContext = osgiBundleContext;
    }


    public NamespaceHandler resolve(String namespaceUri) {
        NamespaceHandler handler;
        for (Iterator i = namespaceHandlerResolvers.iterator(); i.hasNext();) {
					ServiceReference reference = (ServiceReference)i.next();
            try {
                try {
                    NamespaceHandlerResolver resolver =
                            (NamespaceHandlerResolver) osgiBundleContext.getService(reference);
                    if (resolver != null) {
                        handler = resolver.resolve(namespaceUri);
                    } else {
                        handler = null;
                    }
                } finally {
                    osgiBundleContext.ungetService(reference);
                }
            } catch (IllegalArgumentException e) {
                handler = null;
            }
            if (handler != null) {
                return handler;
            }
        }
        throw new IllegalArgumentException(
                "Unable to locate NamespaceHandler for namespace URI [" + namespaceUri + "]");
    }


    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        EntityResolver resolver;
        for (Iterator i = entityResolvers.iterator(); i.hasNext();) {
					ServiceReference reference = (ServiceReference)i.next();
            try {
                resolver = (EntityResolver) osgiBundleContext.getService(reference);
                if (resolver != null) { 
                    InputSource source = resolver.resolveEntity(publicId, systemId);
                    if (source != null) {
                        return source;
                    }
                }
            } finally {
                osgiBundleContext.ungetService(reference);
            }
        }
        return null;
    }


    public void setNamespaceHandlerResolvers(ServiceReference[] namespaceHandlerResolvers) {
        if (namespaceHandlerResolvers != null) {
            for (int i=0; i<namespaceHandlerResolvers.length; i++) {
                this.namespaceHandlerResolvers.add(namespaceHandlerResolvers[i]);
            }
        }
    }


    public void setEntityResolvers(ServiceReference[] entityResolvers) {
        if (entityResolvers != null) {
					for (int i=0; i<entityResolvers.length; i++) {
                this.entityResolvers.add(entityResolvers[i]);
            }
        }
    }
}
