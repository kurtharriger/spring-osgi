package org.springframework.osgi.service;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.osgi.context.BundleContextAware;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * A bean that transparently publishes a bean in the same application context as
 * an OSGi service. <p/> The service properties used when publishing the service
 * are determined by the OsgiServicePropertiesResolver. The default
 * implementation uses
 * <ul>
 * <li>BundleSymbolicName=&lt;bundle symbolic name&gt;</li>
 * <li>BundleVersion=&lt;bundle version&gt;</li>
 * <li>org.springframework.osgi.beanname="&lt;bean name&gt;</li>
 * </ul>
 * 
 * @author Adrian Colyer
 * @author Hal Hildebrand
 * @since 2.0
 */
public class ServiceExporter implements BeanFactoryAware, BeanNameAware, InitializingBean, DisposableBean,
		BundleContextAware {

	private Log log = LogFactory.getLog(OsgiServiceExporter.class);

	private BundleContext bundleContext;
	private OsgiServicePropertiesResolver resolver;
	private BeanFactory beanFactory;
	private ServiceRegistration publishedService;
	private String exportedBean;
	/*
	 * @SuppressWarnings({"FieldCanBeLocal", "UNUSED_SYMBOL"})
	 */
	private String beanName;
	private ArrayList serviceTypes = new ArrayList();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

	public void setExportedBean(String exportedBean) {
		this.exportedBean = exportedBean;
	}

	public String getBeanName() {
		return beanName;
	}

	/**
	 * Set the types to publish the service as
	 * 
	 * @param serviceTypes - a comma sepparated list of fully qualified
	 *            interface class names
	 */
	public void setServiceTypes(String serviceTypes) {
		StringTokenizer tokes = new StringTokenizer(serviceTypes, ",");
		while (tokes.hasMoreTokens()) {
			this.serviceTypes.add(tokes.nextToken().trim());
		}
	}

	/**
	 * @return Returns the resolver.
	 */
	public OsgiServicePropertiesResolver getResolver() {
		return this.resolver;
	}

	/**
	 * @param resolver The resolver to set.
	 */
	public void setResolver(OsgiServicePropertiesResolver resolver) {
		this.resolver = resolver;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		if (this.beanFactory == null) {
			throw new IllegalArgumentException("Required property beanFactory has not been set");
		}
		if (this.bundleContext == null) {
			throw new IllegalArgumentException("Required property bundleContext has not been set");
		}
		if (this.resolver == null) {
			BeanNameServicePropertiesResolver myResolver = new BeanNameServicePropertiesResolver();
			myResolver.setBundleContext(bundleContext);
			resolver = myResolver;
		}
		publishBean();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		publishedService.unregister();
	}

	private void publishBean() throws NoSuchBeanDefinitionException {
		Object bean = beanFactory.getBean(exportedBean);
		Properties serviceProperties = this.resolver.getServiceProperties(exportedBean);
		if (serviceTypes.isEmpty()) {
			serviceTypes.add(bean.getClass().getName());
		}
		publishedService = bundleContext.registerService(
				(String[]) serviceTypes.toArray(new String[serviceTypes.size()]), bean, serviceProperties);
		log.info("Published service type: " + serviceTypes);
	}

}
