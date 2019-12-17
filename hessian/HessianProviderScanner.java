import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.remoting.caucho.HessianServiceExporter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * hessian注解扫描类
 *
 * @author wucc
 */
public class HessianProviderScanner implements BeanFactoryPostProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory)
            throws BeansException {
        logger.info("start export hessian provider");
        String[] beanNames = configurableListableBeanFactory.getBeanNamesForAnnotation(HessianProvider.class);
        for (String beanName : beanNames) {
            try {
                String className = configurableListableBeanFactory.getBeanDefinition(beanName).getBeanClassName();
                Class<?> cls = Class.forName(className);
                HessianProvider anno = cls.getAnnotation(HessianProvider.class);
                if (!StringUtils.isBlank(anno.value())) {
                    beanName = anno.value();
                }
                String hessainServiceName = "/" + beanName.replace("Impl", "");
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.
                        rootBeanDefinition(HessianServiceExporter.class);
                Class<?>[] clsInterfaces = cls.getInterfaces();
                if (clsInterfaces == null || clsInterfaces.length != 1) {
                    continue;
                }
                Class<?> interfaces = clsInterfaces[0];
                builder.addPropertyReference("service", beanName);
                builder.addPropertyValue("serviceInterface", interfaces.getName());
                for (Method method : HessianProvider.class.getDeclaredMethods()) {
                    if (!"equals,toString,hashCode,annotationType,value".contains(method.getName())) {
                        try {
                            String prop = method.getName();
                            Object object = method.invoke(anno);
                            if (StringUtils.isNotBlank(prop) && object != null) {
                                builder.addPropertyValue(prop, object);
                                logger.info("add property [" + prop + "]:" + object.toString());
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            logger.error(method + " invoke error");
                        }
                    }
                }
                ((BeanDefinitionRegistry) configurableListableBeanFactory)
                        .registerBeanDefinition(hessainServiceName, builder.getBeanDefinition());
                logger.info("service:" + beanName + ",serviceInterface:" + interfaces.getName());
            } catch (ClassNotFoundException e) {
                throw new BeanInitializationException(e.getMessage(), e);
            }
        }
        logger.info("export hessian provider end");
    }
}
