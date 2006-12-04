package org.springframework.osgi.context.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.osgi.config.OsgiConfigDefinitionParser;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.util.Assert;

/**
 * @author Hal Hildebrand
 *         Date: Nov 2, 2006
 *         Time: 8:31:33 AM
 */
public class OsgiConfig implements InitializingBean, BeanFactoryAware, BundleContextAware {
	private String pid;
	private List listeners;
	private BeanFactory beanFactory;
	private boolean factory = false;
	private BundleContext bundleContext;


	public void setListeners(List listeners) {
		this.listeners = listeners;
	}


	public void setPid(String pid) {
		this.pid = pid;
	}


	public void setFactory(boolean factory) {
		this.factory = factory;
	}


	public void afterPropertiesSet() throws Exception {
		Assert.notNull(pid, OsgiConfigDefinitionParser.PERSISTENT_ID + " property is required");

		for (Iterator l = listeners.iterator(); l.hasNext();) {
			ConfigListener listener = (ConfigListener) l.next();
			listener.resolve(beanFactory, factory);
		}

		Hashtable props = new Hashtable();
		props.put(Constants.SERVICE_PID, pid);
		if (factory) {
			bundleContext.registerService(ManagedServiceFactory.class.getName(), new FactoryUpdater(), props);
		}
		else {
			bundleContext.registerService(ManagedService.class.getName(), new Updater(), props);
		}
	}


	public void setBundleContext(BundleContext context) {
		bundleContext = context;
	}


	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}


	private class Updater implements ManagedService {
		public void updated(Dictionary properties) throws ConfigurationException {
			for (Iterator l = listeners.iterator(); l.hasNext();) {
				ConfigListener listener = (ConfigListener) l.next();
				listener.updated(properties, pid);
			}
		}
	}

	private class FactoryUpdater implements ManagedServiceFactory {
		public void deleted(String instancePid) {
			for (Iterator l = listeners.iterator(); l.hasNext();) {
				ConfigListener listener = (ConfigListener) l.next();
				listener.deleted(instancePid);
			}
		}


		public String getName() {
			return "Managed service factory updater for: [" + pid + "]";
		}


		public void updated(String instancePid, Dictionary properties) throws ConfigurationException {
			for (Iterator l = listeners.iterator(); l.hasNext();) {
				ConfigListener listener = (ConfigListener) l.next();
				listener.updated(instancePid, properties);
			}
		}
	}


	public static class ConfigListener {
		private String reference;
		private String updateMethod;
		private String deletedMethod;
		private Object bean;


		public void setReference(String reference) {
			this.reference = reference;
		}


		public void setUpdateMethod(String updateMethod) {
			this.updateMethod = updateMethod;
		}


		public void setDeletedMethod(String deletedMethod) {
			this.deletedMethod = deletedMethod;
		}


		void resolve(BeanFactory beanFactory, boolean isFactory) {
			bean = beanFactory.getBean(reference);
			if (isFactory) {
				try {
					bean.getClass().getMethod(updateMethod, new Class[]{String.class, Map.class});
				}
				catch (NoSuchMethodException e) {
					throw (IllegalStateException)new IllegalArgumentException("Invalid or missing update method for bean " + reference
						+ "; requires signature (java.lang.String, java.util.Map)").initCause(e);
				}
			}
			else {
				try {
					bean.getClass().getMethod(updateMethod, new Class[]{Map.class});
				}
				catch (NoSuchMethodException e) {
					throw (IllegalStateException) new IllegalArgumentException("Invalid or missing update method for bean " + reference
						+ "; requires signature (java.util.Map)").initCause(e);
				}
			}
			if (deletedMethod != null) {
				try {
					bean.getClass().getMethod(deletedMethod, new Class[]{String.class});
				}
				catch (NoSuchMethodException e) {
					throw (IllegalStateException)new IllegalArgumentException("Invalid or missing deleted method for bean " + reference
						+ "; requires signature (java.lang.String)").initCause(e);
				}
			}
		}


		void updated(String instancePid, Dictionary properties) throws ConfigurationException {
			Method update;
			try {
				update = bean.getClass().getMethod(updateMethod, new Class[]{String.class, Map.class});
			}
			catch (NoSuchMethodException e) {
				throw new ConfigurationException(instancePid,
					"Invalid or missing update method for bean " + reference
						+ "; requires signature (java.util.String, java.util.Map)",
					e);
			}

			try {
				update.invoke(bean, new Object[]{instancePid, properties});
			}
			catch (IllegalAccessException e) {
				throw new ConfigurationException(instancePid, "Insufficient permission to invoke update method", e);
			}
			catch (InvocationTargetException e) {
				throw new ConfigurationException(instancePid, "Error updating", e.getTargetException());
			}
		}


		void updated(Dictionary properties, String servicePid) throws ConfigurationException {
			Method update;
			try {
				update = bean.getClass().getMethod(updateMethod, new Class[]{Map.class});
			}
			catch (NoSuchMethodException e) {
				throw new ConfigurationException(servicePid, "Invalid or missing update method for bean " + reference
					+ "; requires signature (java.util.Map)", e);
			}

			try {
				update.invoke(bean, new Object[]{properties});
			}
			catch (IllegalAccessException e) {
				throw new ConfigurationException(servicePid, "Insufficient permission to invoke update method", e);
			}
			catch (InvocationTargetException e) {
				throw new ConfigurationException(servicePid, "Error updating", e.getTargetException());
			}
		}


		void deleted(String instancePid) {
			if (deletedMethod == null) {
				return;
			}
			Method deleted;
			try {
				deleted = bean.getClass().getMethod(deletedMethod, new Class[]{String.class});
			}
			catch (NoSuchMethodException e) {
				throw (IllegalStateException)new IllegalStateException("Invalid or missing deleted method for bean " + reference
					+ "; requires signature (java.lang.String)").initCause(e);
			}

			try {
				deleted.invoke(bean, new Object[]{instancePid});
			}
			catch (IllegalAccessException e) {
				throw (IllegalStateException)new IllegalStateException("Insufficient permission to invoke deleted method").initCause(e);
			}
			catch (InvocationTargetException e) {
				throw (IllegalStateException)new IllegalStateException("Error deleting").initCause(e.getTargetException());
			}
		}
	}
}
