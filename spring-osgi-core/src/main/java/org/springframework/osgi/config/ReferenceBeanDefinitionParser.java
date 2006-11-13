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
package org.springframework.osgi.config;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.osgi.config.ParserUtils.AttributeCallback;
import org.springframework.osgi.service.OsgiServiceProxyFactoryBean;
import org.springframework.osgi.service.TargetSourceLifecycleListener;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Andy Piper
 * @author Costin Leau
 * @since 2.1
 */
class ReferenceBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

	public static final String PROPERTIES = "properties";
	public static final String LISTENER = "listener";
	public static final String LISTENERS_PROPERTY = "listeners";
	public static final String BIND_METHOD = "bind-method";
	public static final String UNBIND_METHOD = "unbind-method";
	public static final String REF = "ref";
    public static final String INTERFACE = "interface";
    public static final String INTERFACE_NAME = "interfaceName";

    static class ServiceListenerWrapper implements TargetSourceLifecycleListener, InitializingBean, BeanClassLoaderAware {
		private Method bind, unbind;
		private String bindMethod, unbindMethod;
		private final Class[] METHODS_ARGS = new Class[] { String.class, Object.class };
		private final Class[] METHODS_ARGS2 = new Class[] { Object.class };
		private Object target;
        private String interfaceName;
        private Class interfaceClass;
        private ClassLoader classLoader;

        private boolean isLifecycleListener;

		public ServiceListenerWrapper(Object object) {
			this.target = object;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
		 */
		public void afterPropertiesSet() throws Exception {
			Assert.notNull(target, "target property required");
            Assert.notNull(interfaceName, "interface property required");
            
            interfaceClass = classLoader.loadClass(interfaceName);

			isLifecycleListener = target instanceof TargetSourceLifecycleListener;

			bind = determineCustomMethod(bindMethod);
			unbind = determineCustomMethod(unbindMethod);

			if (!isLifecycleListener && (bind == null || unbind == null))
				throw new IllegalArgumentException("target object needs to implement "
						+ TargetSourceLifecycleListener.class + " or custom bind/unbind methods have to be specified");
        }

		/**
		 * Determine a custom method (if specified) on the given object. If the
		 * methodName is not null and no method is found, an exception is
		 * thrown.
		 * 
		 * @param methodName
		 * @return
		 */
		private Method determineCustomMethod(String methodName) {
			if (methodName == null) {
                return null;
            }

            Method method;
            method = ReflectionUtils.findMethod(target.getClass(), methodName, new Class[] {String.class, 
                                                                                            interfaceClass});
            if (method != null) {
                return method;
            }
            
            method = ReflectionUtils.findMethod(target.getClass(), methodName, new Class[] {interfaceClass});
			if (method != null) {
                return method;
            }

            method = ReflectionUtils.findMethod(target.getClass(), methodName, METHODS_ARGS);
			if (method != null) {
                return method;
            }

            method = ReflectionUtils.findMethod(target.getClass(), methodName, METHODS_ARGS2);
			if (method != null) {
                return method;
            }

            throw new IllegalArgumentException("incorrect custom method specified " +
                                               methodName + " on class " + target.getClass());
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.osgi.service.TargetSourceLifecycleListener#bind(java.lang.String,
		 *      java.lang.Object)
		 */
		public void bind(String serviceBeanName, Object service) {
			// first call interface method (if it exists)
			if (isLifecycleListener)
				((TargetSourceLifecycleListener) target).bind(serviceBeanName, service);

			if (bind == null) {
                return;
            }
            try {
                ReflectionUtils.invokeMethod(bind, target, new Object[] { serviceBeanName, service });
            } catch (IllegalArgumentException e) {
                ReflectionUtils.invokeMethod(bind, target, new Object[] { service });
            }
        }

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.osgi.service.TargetSourceLifecycleListener#unbind(java.lang.String,
		 *      java.lang.Object)
		 */
		public void unbind(String serviceBeanName, Object service) {
			// first call interface method (if it exists)
			if (isLifecycleListener)
				((TargetSourceLifecycleListener) target).unbind(serviceBeanName, service);

			if (unbind == null) {
                return;
            }
            try {
                ReflectionUtils.invokeMethod(unbind, target, new Object[] { serviceBeanName, service });
            } catch (IllegalArgumentException e) {
                ReflectionUtils.invokeMethod(unbind, target, new Object[] { service });
            }

        }

		/**
		 * @param bindMethod The bindMethod to set.
		 */
		public void setBindMethod(String bindMethod) {
			this.bindMethod = bindMethod;
		}

		/**
		 * @param unbindMethod The unbindMethod to set.
		 */
		public void setUnbindMethod(String unbindMethod) {
			this.unbindMethod = unbindMethod;
		}


        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }


        public void setBeanClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser#getBeanClass(org.w3c.dom.Element)
	 */
	protected Class getBeanClass(Element element) {
		return OsgiServiceProxyFactoryBean.class;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser#doParse(org.w3c.dom.Element,
	 *      org.springframework.beans.factory.xml.ParserContext,
	 *      org.springframework.beans.factory.support.BeanDefinitionBuilder)
	 */
	protected void doParse(Element element, ParserContext context, BeanDefinitionBuilder builder) {
		AbstractBeanDefinition def = builder.getBeanDefinition();
        String interfaceName = element.getAttribute(INTERFACE);

        ParserUtils.parseCustomAttributes(element, builder, new AttributeCallback() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.springframework.osgi.config.ParserUtils.AttributeCallback#process(org.w3c.dom.Element,
			 *      org.w3c.dom.Attr,
			 *      org.springframework.beans.factory.support.BeanDefinitionBuilder)
			 */
			public void process(Element parent, Attr attribute, BeanDefinitionBuilder builder) {
				String name = attribute.getLocalName();
                // ref attribute will be handled separately
				builder.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
			}
		});

		// parse subelements
		// context.getDelegate().parsePropertyElements(element,
		// builder.getBeanDefinition());
		List listeners = DomUtils.getChildElementsByTagName(element, LISTENER);

		ManagedList listenersRef = new ManagedList();
		// loop on listeners
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			Element listnr = (Element) iter.next();

			// wrapper target object
			Object target = null;

			// filter elements
			NodeList nl = listnr.getChildNodes();

			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				if (node instanceof Element) {
					Element beanDef = (Element) node;

					// check inline ref
					if (listnr.hasAttribute(REF))
						context.getReaderContext().error(
								"nested bean declaration is not allowed if 'ref' attribute has been specified", beanDef);

					target = context.getDelegate().parsePropertySubElement(beanDef, def);
				}
			}


			// extract bind/unbind attributes from <osgi:listener>
			// Element

			MutablePropertyValues vals = new MutablePropertyValues();

			NamedNodeMap attrs = listnr.getAttributes();
			for (int x = 0; x < attrs.getLength(); x++) {
				Attr attribute = (Attr) attrs.item(x);
				String name = attribute.getLocalName();

				if (REF.equals(name))
					target = new RuntimeBeanReference(StringUtils.trimWhitespace(attribute.getValue()));
				else
					vals.addPropertyValue(Conventions.attributeNameToPropertyName(name), attribute.getValue());
			}
            vals.addPropertyValue(INTERFACE_NAME, interfaceName);

            // create serviceListener wrapper
			RootBeanDefinition wrapperDef = new RootBeanDefinition(ServiceListenerWrapper.class);

			ConstructorArgumentValues cav = new ConstructorArgumentValues();
			cav.addIndexedArgumentValue(0, target);

			wrapperDef.setConstructorArgumentValues(cav);
			wrapperDef.setPropertyValues(vals);
            listenersRef.add(wrapperDef);

		}

		def.getPropertyValues().addPropertyValue(LISTENERS_PROPERTY, listenersRef);
	}
}
