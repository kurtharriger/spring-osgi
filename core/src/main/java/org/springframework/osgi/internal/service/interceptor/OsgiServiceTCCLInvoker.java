package org.springframework.osgi.internal.service.interceptor;

import java.lang.reflect.InvocationTargetException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author Hal Hildebrand
 *         Date: Apr 13, 2007
 *         Time: 6:43:34 PM
 */
public class OsgiServiceTCCLInvoker implements MethodInterceptor {
    protected Object target;
    protected ClassLoader loader;


    public OsgiServiceTCCLInvoker(Object target, ClassLoader loader) {
        this.target = target;
        this.loader = loader;
    }


    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);
            try {
                return methodInvocation.getMethod().invoke(target, methodInvocation.getArguments());
            }
            catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
