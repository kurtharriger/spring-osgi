/*
 * Copyright 2006 the original author or authors.
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
 */
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
import java.io.FileNotFoundException;
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
                InputSource is = null;
                try {
                    is = ((Plugin) i.next()).resolveEntity(publicId, systemId);
                } catch (FileNotFoundException e) {
                    // ignore
                }
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
