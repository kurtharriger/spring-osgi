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

import java.net.URL;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author Adrian Colyer
 */
public class OsgiResourceUtils
{
  private static final char PREFIX_SEPARATOR = ':';
  private static final String ABSOLUTE_PATH_PREFIX = "/";

  protected static boolean isRelativePath(String locationPath) {
    return ((locationPath.indexOf(PREFIX_SEPARATOR) == -1) &&
        !locationPath.startsWith(ABSOLUTE_PATH_PREFIX));
  }

  /**
   * Resolves a resource from *this bundle only*. Only the bundle and its
   * attached fragments are searched for the given resource.
   *
   * @param bundleRelativePath
   * @return Resource
   */
  protected static Resource getResourceFromBundle(String bundleRelativePath, Bundle bundle) {
    URL entry = bundle.getEntry(bundleRelativePath);
    if (entry != null) {
      return new UrlResource(entry);
    } else {
      throw new IllegalArgumentException("Unable to find resource: " + bundleRelativePath + " in bundle: " + bundle);
    }
  }

  /**
   * Resolves a resource from the bundle's classpath. This will find resources
   * in this bundle and also in imported packages from other bundles.
   *
   * @param bundleRelativePath
   * @return Resource
   */
  protected static Resource getResourceFromBundleClasspath(String bundleRelativePath, Bundle bundle) {
    URL resource = bundle.getResource(bundleRelativePath);
    if (resource != null) {
      return new UrlResource(resource);
    } else {
      throw new IllegalArgumentException("Unable to find resource: " + bundleRelativePath + " in bundle: " + bundle);
    }
  }

  // FIXME: Ugly hack - should be standardized somehow
  public static BundleContext getBundleContext(Bundle bundle) {
    if (bundle == null) return null;
    try {
// Retrieve bundle context from Equinox
      Method m = bundle.getClass().getDeclaredMethod("getContext", new Class[0]);
      m.setAccessible(true);
      return (BundleContext) m.invoke(bundle, new Object[0]);
    } catch (Exception exc) {
// Retrieve bundle context from Knopflerfish
      try {
        Field[] fields = bundle.getClass().getDeclaredFields();
        Field f = null;
        for (int i = 0; i < fields.length; i++) {
          if (fields[i].getName().equals("bundleContext")) {
            f = fields[i];
            break;
          }
        }
        if (f == null) {
          throw new IllegalStateException("No bundleContext field!");
        }
        f.setAccessible(true);
        return (BundleContext) f.get(bundle);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Exception retrieving bundle context", e);
      }
    }
  }
}
