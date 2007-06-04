package org.springframework.osgi.extender.support;

import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.context.event.ApplicationEventMulticaster;

import java.util.Timer;

/**
 * @author Hal Hildebrand
 *         Date: May 29, 2007
 *         Time: 7:39:17 PM
 */
public interface ServiceDependentOsgiApplicationContext extends ConfigurableOsgiBundleApplicationContext {


    void setExecutor(TaskExecutor executor);
    

    void setTimer(Timer timer);


    void setTimeout(long timeout);


    void setMcast(ApplicationEventMulticaster mcast);


    ContextState getState();
}
