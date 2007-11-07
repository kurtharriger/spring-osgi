package org.springframework.osgi.internal.service.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.ObjectUtils;

/**
 * Simple interceptor for dealing with ThreadContextClassLoader(TCCL)
 * management.
 * 
 * @author Hal Hildebrand
 * @author Costin Leau
 */
public class OsgiServiceTCCLInterceptor implements MethodInterceptor {
	/** classloader to set the TCCL during invocation */
	private final ClassLoader loader;

	/**
	 * Constructs a new <code>OsgiServiceTCCLInterceptor</code> instance.
	 * 
	 * @param loader classloader to use for TCCL during invocation. Can be null.
	 */
	public OsgiServiceTCCLInterceptor(ClassLoader loader) {
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
		if (other instanceof OsgiServiceTCCLInterceptor) {
			OsgiServiceTCCLInterceptor oth = (OsgiServiceTCCLInterceptor) other;
			return (ObjectUtils.nullSafeEquals(loader, oth.loader));
		}
		return false;
	}
}
