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
package org.springframework.osgi.internal.config;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.osgi.bundle.BundleFactoryBean;
import org.springframework.osgi.bundle.ChainedBundleAction;
import org.springframework.osgi.bundle.InstallBundleAction;
import org.springframework.osgi.bundle.StartBundleAction;
import org.springframework.osgi.bundle.StopBundleAction;
import org.springframework.osgi.bundle.UninstallBundleAction;
import org.springframework.osgi.bundle.UpdateBundleAction;
import org.springframework.osgi.internal.config.ParserUtils.AttributeCallback;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * BundleFactoryBean definition.
 * 
 * @author Andy Piper
 * @author Costin Leau
 */
class BundleBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	static class BundleActionCallback implements AttributeCallback {

		private final ParserContext parserContext;

		public BundleActionCallback(ParserContext parserContext) {
			this.parserContext = parserContext;
		}

		private static final String INSTALL = "install";

		private static final String START = "start";

		private static final String UPDATE = "update";

		private static final String STOP = "stop";

		private static final String UNINSTALL = "uninstall";

		private static final String LOCATION = "location";

		public boolean process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
			String name = attribute.getLocalName();
			if (ACTION.equals(name)) {
				builder.addPropertyValue(ACTION_PROP, parseAction(parent, attribute));
				return false;
			}

			if (DESTROY_ACTION.equals(name)) {
				builder.addPropertyValue(DESTROY_ACTION_PROP, parseAction(parent, attribute));
				return false;
			}

			return true;
		}

		private BeanDefinition parseAction(Element parent, Attr attribute) {
			String name = attribute.getLocalName();
			String action = attribute.getValue();

			if (INSTALL.equalsIgnoreCase(action)) {
				return createInstallBeanDef(parent);
			}

			else if (START.equalsIgnoreCase(action)) {
				// start implies install

				RootBeanDefinition def = new RootBeanDefinition(ChainedBundleAction.class);
				ConstructorArgumentValues values = new ConstructorArgumentValues();
				ManagedList list = new ManagedList();
				list.add(createInstallBeanDef(parent));
				list.add(createStartBeanDef());
				values.addGenericArgumentValue(list);
				def.setConstructorArgumentValues(values);
				return def;
			}

			else if (UPDATE.equalsIgnoreCase(action)) {

				// update implies install (
				RootBeanDefinition def = new RootBeanDefinition(ChainedBundleAction.class);
				ConstructorArgumentValues values = new ConstructorArgumentValues();
				ManagedList list = new ManagedList();
				list.add(createInstallBeanDef(parent));
				list.add(createUpdateBeanDef());
				values.addGenericArgumentValue(list);
				def.setConstructorArgumentValues(values);
				return def;
			}

			else if (STOP.equalsIgnoreCase(action)) {
				return new RootBeanDefinition(StopBundleAction.class);
			}

			else if (UNINSTALL.equalsIgnoreCase(action)) {

				return new RootBeanDefinition(UninstallBundleAction.class);
			}

			parserContext.getReaderContext().error("invalid value=[" + action + "] for attribute [" + name + "]",
				attribute);

			return null;
		}

		private RootBeanDefinition createInstallBeanDef(Element element) {
			RootBeanDefinition def = new RootBeanDefinition(InstallBundleAction.class);
			MutablePropertyValues values = new MutablePropertyValues();
			values.addPropertyValue(LOCATION, element.getAttribute(LOCATION));
			def.setPropertyValues(values);
			return def;
		}

		private RootBeanDefinition createStartBeanDef() {
			return new RootBeanDefinition(StartBundleAction.class);
		}

		private RootBeanDefinition createUpdateBeanDef() {
			return new RootBeanDefinition(UpdateBundleAction.class);
		}

	};

	private static final String ACTION = "action";

	private static final String DESTROY_ACTION = "destroy-action";

	// class properties

	private static final String ACTION_PROP = "action";

	private static final String DESTROY_ACTION_PROP = "destroyAction";

	private static final String BUNDLE_PROP = "bundle";

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		BundleActionCallback callback = new BundleActionCallback(parserContext);

		ParserUtils.parseCustomAttributes(element, builder, new AttributeCallback[] { callback });

		// parse nested definition (in case there is any)

		if (element.hasChildNodes()) {
			NodeList nodes = element.getChildNodes();
			boolean foundElement = false;
			for (int i = 0; i < nodes.getLength() && !foundElement; i++) {
				Node nd = nodes.item(i);
				if (nd instanceof Element) {
					foundElement = true;
					Object obj = parserContext.getDelegate().parsePropertySubElement((Element) nd,
						builder.getBeanDefinition());
					builder.addPropertyValue(BUNDLE_PROP, obj);
				}
			}
		}
	}

	protected Class getBeanClass(Element element) {
		return BundleFactoryBean.class;
	}

}
