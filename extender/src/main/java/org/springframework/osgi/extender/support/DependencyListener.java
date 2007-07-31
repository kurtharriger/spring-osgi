package org.springframework.osgi.extender.support;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.service.importer.AbstractOsgiServiceProxyFactoryBean;

/**
 * ServiceListener used for tracking dependent services. As the ServiceListener
 * receives event synchronously there is no need for synchronization.
 * 
 * @author Costin Leau
 * @author Hal Hildebrand
 */
public class DependencyListener implements ServiceListener {
	protected final Set dependencies = new LinkedHashSet();

	protected final Set unsatisfiedDependencies = new LinkedHashSet();

	protected ServiceDependentOsgiBundleXmlApplicationContext context;

	private static final Log log = LogFactory.getLog(DependencyListener.class);

	public DependencyListener(ServiceDependentOsgiBundleXmlApplicationContext context) {
		this.context = context;
	}

	protected void findServiceDependencies() {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory,
			AbstractOsgiServiceProxyFactoryBean.class, true, true);
		for (int i = 0; i < beans.length; i++) {
			String beanName = BeanFactory.FACTORY_BEAN_PREFIX + beans[i];
			AbstractOsgiServiceProxyFactoryBean reference = (AbstractOsgiServiceProxyFactoryBean) beanFactory.getBean(beanName);
			Dependency dependency = new Dependency(context.getBundleContext(), reference);
			dependencies.add(dependency);
			if (!dependency.isSatisfied()) {
				unsatisfiedDependencies.add(dependency);
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(dependencies.size() + " dependencies, " + unsatisfiedDependencies.size() + " unsatisfied for "
					+ context.getDisplayName());
		}

	}

	protected boolean isSatisfied() {
		return unsatisfiedDependencies.isEmpty();
	}

	public Set getUnsatisfiedDependencies() {
		return unsatisfiedDependencies;
	}

	protected void register() {
		boolean multiple = unsatisfiedDependencies.size() > 1;
		StringBuffer sb = new StringBuffer(100 * unsatisfiedDependencies.size());
		if (multiple) {
			sb.append("(|");
		}
		for (Iterator i = unsatisfiedDependencies.iterator(); i.hasNext();) {
			((Dependency) i.next()).appendTo(sb);
		}
		if (multiple) {
			sb.append(')');
		}
		String filter = sb.toString();
		if (log.isInfoEnabled()) {
			log.info(context.getDisplayName() + " registering listener with filter: " + filter);
		}
		try {
			context.getBundleContext().addServiceListener(this, filter);
		}
		catch (InvalidSyntaxException e) {
			throw (IllegalStateException) new IllegalStateException("Filter string '" + filter
					+ "' has invalid syntax: " + e.getMessage()).initCause(e);
		}
	}

	protected void deregister() {
		if (log.isDebugEnabled()) {
			log.debug("deregistering listener for " + context.getDisplayName());
		}
		try {
			context.getBundleContext().removeServiceListener(this);
		}
		catch (IllegalStateException e) {
			// Bundle context is no longer valid
		}
	}

	/**
	 * Process serviceChanged events, completing context initialization if all
	 * the required dependencies are satisfied.
	 * 
	 * @param serviceEvent
	 */
	public void serviceChanged(ServiceEvent serviceEvent) {
		try {
			if (unsatisfiedDependencies.isEmpty()) { // already completed.
				if (log.isDebugEnabled()) {
					log.debug("handling service event, but no unsatisfied dependencies for " + context.getDisplayName());
				}
				return;
			}
			if (log.isDebugEnabled()) {
				log.debug("handling service event [" + typeStringFor(serviceEvent.getType()) + ":"
						+ toString(serviceEvent.getServiceReference()) + "] for " + context.getDisplayName());
			}

			for (Iterator i = dependencies.iterator(); i.hasNext();) {
				Dependency dependency = (Dependency) i.next();
				if (dependency.matches(serviceEvent)) {
					switch (serviceEvent.getType()) {
					case ServiceEvent.MODIFIED:
					case ServiceEvent.REGISTERED:
						unsatisfiedDependencies.remove(dependency);
						if (log.isDebugEnabled()) {
							log.debug("found service; eliminating " + dependency);
						}
						break;
					case ServiceEvent.UNREGISTERING:
						unsatisfiedDependencies.add(dependency);
						if (log.isDebugEnabled()) {
							log.debug("service unregistered; adding " + dependency);
						}
						break;
					default: // do nothing
						if (log.isDebugEnabled()) {
							log.debug("Unknown service event type for: " + dependency);
						}
						break;
					}
				}
				else {
					if (log.isDebugEnabled()) {
						log.debug(dependency + " does not match");
					}
				}
			}

			if (context.getState() == ContextState.INTERRUPTED || context.getState() == ContextState.CLOSED) {
				deregister();
				return;
			}

			// Good to go!
			if (unsatisfiedDependencies.isEmpty()) {
				deregister();
				context.listener = null;
				if (log.isDebugEnabled()) {
					log.debug("No outstanding dependencies, completing initialization for " + context.getDisplayName());
				}
				context.dependenciesAreSatisfied(true);
			}
			else {
				register(); // re-register with the new filter
			}
		}
		catch (Throwable e) {
			// frameworks will simply not log exception for event handlers
			log.fatal("Exception during dependency processing for " + context.getDisplayName(), e);
		}
	}

	protected String typeStringFor(int event) {
		switch (event) {
		case ServiceEvent.MODIFIED:
			return "MODIFIED";
		case ServiceEvent.REGISTERED:
			return "REGISTERED";
		case ServiceEvent.UNREGISTERING:
			return "UNREGISTERING";
		default:
			return "Unknown ServiceEvent type";
		}
	}

	protected String toString(ServiceReference ref) {
		StringBuffer buf = new StringBuffer();
		buf.append("ServiceReference [").append(ref.getBundle().getSymbolicName()).append("] ");
		String clazzes[] = (String[]) ref.getProperty(Constants.OBJECTCLASS);
		int size = clazzes.length;
		buf.append('{');
		for (int i = 0; i < size; i++) {
			if (i > 0)
				buf.append(", ");
			buf.append(clazzes[i]);
		}

		buf.append("}={");
		String[] keys = ref.getPropertyKeys();
		for (int i = 0; i < keys.length; i++) {
			if (!Constants.OBJECTCLASS.equals(keys[i])) {
				buf.append(keys[i]).append('=').append(ref.getProperty(keys[i])).append(',');
			}
		}
		buf.append('}');

		return buf.toString();
	}
}