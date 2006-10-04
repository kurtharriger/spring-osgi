package org.springframework.osgi.context.support;

import org.osgi.framework.Bundle;
import org.springframework.beans.factory.xml.DefaultNamespaceHandlerResolver;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Hal Hildebrand
 *         Date: Aug 23, 2006
 *         Time: 8:32:49 PM
 */
public class NamespacePlugins implements NamespaceHandlerResolver, EntityResolver {
    private final Map plugins = new HashMap();


    public void addHandler(Bundle bundle) {
        synchronized (plugins) {
            //noinspection unchecked
            plugins.put(bundle, new Plugin(new BundleDelegatingClassLoader(bundle)));
        }
    }


    public void removeHandler(Bundle bundle) {
        synchronized (plugins) {
            plugins.remove(bundle);
        }
    }


    public NamespaceHandler resolve(String namespaceUri) {
        synchronized (plugins) {
            for (Iterator i = plugins.values().iterator(); i.hasNext();) {
                try {
                    NamespaceHandler handler = ((Plugin) i.next()).resolve(namespaceUri);
                    if (handler != null) {
                        return handler;
                    }
                } catch (IllegalArgumentException e) {
                    // This is thrown when the DefaultNamespaceHandlerResolver is unable to resolve the namespace
                }
            }
            return null;
        }
    }


    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        synchronized (plugins) {
            for (Iterator i = plugins.values().iterator(); i.hasNext();) {
                InputSource is = ((Plugin) i.next()).resolveEntity(publicId, systemId);
                if (is != null) {
                    return is;
                }
            }
            return null;
        }
    }


    private class Plugin implements NamespaceHandlerResolver, EntityResolver {
        private final NamespaceHandlerResolver namespace;
        private final EntityResolver entity;


        private Plugin(ClassLoader loader) {
            entity = new PluggableSchemaResolver(loader);
            namespace = new DefaultNamespaceHandlerResolver(loader);

        }


        public NamespaceHandler resolve(String namespaceUri) {
            return namespace.resolve(namespaceUri);
        }


        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return entity.resolveEntity(publicId, systemId);
        }
    }
}
