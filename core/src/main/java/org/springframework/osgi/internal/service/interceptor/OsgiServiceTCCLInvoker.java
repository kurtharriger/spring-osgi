package org.springframework.osgi.internal.service.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.ObjectUtils;

/**
 * Simple interceptor for dealing with ThreadContextClassLoader management.
 * 
 * @author Hal Hildebrand
 * @author Costin Leau
 */
public class OsgiServiceTCCLInvoker implements MethodInterceptor {
	protected final ClassLoader loader;

	public OsgiServiceTCCLInvoker(ClassLoader loader) {
		this.loader = loader;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		ClassLoader previous = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(loader);
			return invocation.proceed();
		}
		finally {
			Thread.currentThread().setContextClassLoader(previous);
		}
	}

	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (other instanceof OsgiServiceTCCLInvoker) {
			OsgiServiceTCCLInvoker oth = (OsgiServiceTCCLInvoker) other;
			return (ObjectUtils.nullSafeEquals(loader, oth.loader));
		}
		return false;
	}
}
