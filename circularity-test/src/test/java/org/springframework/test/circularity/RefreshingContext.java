package org.springframework.test.circularity;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Hal Hildebrand
 *         Date: Apr 13, 2007
 *         Time: 9:27:07 PM
 */
public class RefreshingContext extends AbstractXmlApplicationContext {
    public List factories = new ArrayList();
    private Resource[] configResources;


    public RefreshingContext(String[] paths, Class clazz)
            throws BeansException {

        super(null);
        Assert.notNull(paths, "Path array must not be null");
        Assert.notNull(clazz, "Class argument must not be null");
        configResources = new Resource[paths.length];
        for (int i = 0; i < paths.length; i++) {
            configResources[i] = new ClassPathResource(paths[i], clazz);
        }
        refresh();
    }


    protected Resource[] getConfigResources() {
        return configResources;
    }


    protected void onRefresh() throws BeansException {
        ConfigurableListableBeanFactory factory = getBeanFactory();
        String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(factory,
                                                                             AFactory.class,
                                                                             true,
                                                                             false);
        for (int i = 0; i < beans.length; i++) {
            String beanName =  beans[i];
            AFactory reference = (AFactory) factory.getBean(beanName);
            if (reference.isGetObjectCalled()) {
                throw new IllegalStateException("GetObject has been called");
            }
            factories.add(reference);
        }

        super.onRefresh();
    }
}
