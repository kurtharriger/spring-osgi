/*
 * Copyright 2006-2009 the original author or authors.
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

import java.beans.PropertyEditor;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.propertyeditors.CustomMapEditor;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.Assert;

/**
 * Class that registers {@link PropertyEditor}s, useful inside an OSGi application context.
 * 
 * As this class is used for bootstrapping and is likely to draw classes from various packages that already depend on
 * {@link BundleContextAware}, the configuration has been externalized to avoid package cycles.
 * 
 * @author Costin Leau
 */
class OsgiPropertyEditorRegistrar implements PropertyEditorRegistrar {

	private static final Log log = LogFactory.getLog(OsgiPropertyEditorRegistrar.class);

	private static final String PROPERTIES_FILE =
			"/org/springframework/osgi/context/support/internal/default-property-editors.properties";

	private final Map<Class<?>, Class<? extends PropertyEditor>> editors;

	OsgiPropertyEditorRegistrar() {
		this(OsgiPropertyEditorRegistrar.class.getClassLoader());
	}

	OsgiPropertyEditorRegistrar(ClassLoader classLoader) {
		// load properties
		Properties editorsConfig = new Properties();
		try {
			editorsConfig.load(getClass().getResourceAsStream(PROPERTIES_FILE));
		} catch (IOException ex) {
			throw (RuntimeException) new IllegalStateException("cannot load default property editors configuration")
					.initCause(ex);
		}

		if (log.isTraceEnabled())
			log.trace("Loaded property editors configuration " + editorsConfig);
		editors = new LinkedHashMap<Class<?>, Class<? extends PropertyEditor>>(editorsConfig.size());

		createEditors(classLoader, editorsConfig);
	}

	@SuppressWarnings("unchecked")
	private void createEditors(ClassLoader classLoader, Properties configuration) {

		boolean trace = log.isTraceEnabled();

		for (Map.Entry<Object, Object> entry : configuration.entrySet()) {
			// key represents type
			Class<?> key;
			// value represents property editor
			Class<?> editorClass;
			try {
				key = classLoader.loadClass((String) entry.getKey());
				editorClass = classLoader.loadClass((String) entry.getValue());
			} catch (ClassNotFoundException ex) {
				throw (RuntimeException) new IllegalArgumentException("Cannot load class").initCause(ex);
			}

			Assert.isAssignable(PropertyEditor.class, editorClass);

			if (trace)
				log.trace("Adding property editor[" + editorClass + "] for type[" + key + "]");
			editors.put(key, (Class<? extends PropertyEditor>) editorClass);
		}
	}

	public void registerCustomEditors(PropertyEditorRegistry registry) {
		for (Map.Entry<Class<?>, Class<? extends PropertyEditor>> entry : editors.entrySet()) {
			Class<?> type = entry.getKey();
			PropertyEditor editorInstance;
			editorInstance = BeanUtils.instantiate(entry.getValue());
			registry.registerCustomEditor(type, editorInstance);
		}

		// register non-externalized types
		registry.registerCustomEditor(Dictionary.class, new CustomMapEditor(Hashtable.class));
	}
}