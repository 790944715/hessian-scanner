import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * hessian 接口客户端自动扫描注入
 *
 * @author wucc
 */
public class HessianCustomerScanner implements
        ApplicationContextAware, BeanFactoryPostProcessor {
    /**
     * hessian提供者url，key为注解的provider属性
     */
    private Map<String, String> urls;
    /**
     * 包扫描路径
     */
    private String basePackage;
    private ApplicationContext applicationContext;

    public Map<String, String> getUrls() {
        return urls;
    }

    public void setUrls(Map<String, String> urls) {
        this.urls = urls;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        refresh((BeanDefinitionRegistry) configurableListableBeanFactory);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    private void refresh(BeanDefinitionRegistry registry) {
        AnnotationScanner scanner = new AnnotationScanner(registry);
        scanner.setIncludeAnnotationConfig(true);
        scanner.setResourceLoader(applicationContext);
        scanner.addIncludeFilter(new AnnotationTypeFilter(HessianCustomer.class));
        scanner.scan(StringUtils.splitPreserveAllTokens(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

    /**
     * 扫描注册bean实例
     */
    private class AnnotationScanner extends ClassPathBeanDefinitionScanner {
        protected final Logger logger = LoggerFactory.getLogger(HessianCustomerScanner.class);

        private AnnotationScanner(BeanDefinitionRegistry registry) {
            super(registry, false);
        }

        @Override
        protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
            logger.info("scan hessian customer interface begin");
            logger.info("basePackages:" + Arrays.toString(basePackages));
            Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
            Set<BeanDefinitionHolder> result = new HashSet<>();
            if (!beanDefinitions.isEmpty()) {
                logger.info("start set beanDefinition");
                for (BeanDefinitionHolder holder : beanDefinitions) {
                    GenericBeanDefinition beanDefinition = (GenericBeanDefinition) holder.getBeanDefinition();
                    AnnotationMetadata metadata = ((ScannedGenericBeanDefinition) beanDefinition).getMetadata();
                    Map<String, Object> attrs = metadata.getAnnotationAttributes(HessianCustomer.class.getName());
                    String provider = MapUtils.getString(attrs, "provider");
                    attrs.remove("provider");
                    String beanName = MapUtils.getString(attrs, "value");
                    attrs.remove("value");
                    String ref = MapUtils.getString(attrs, "ref");
                    attrs.remove("ref");
                    if (!urls.containsKey(provider)) {
                        logger.error("provider:[" + provider + "] is not config");
                        this.getRegistry().removeBeanDefinition(holder.getBeanName());
                        continue;
                    }
                    String url = MapUtils.getString(urls, provider);
                    if (!url.endsWith("/") && !ref.startsWith("/")) url += "/" + ref;
                    beanName = StringUtils.isNotBlank(beanName) ? beanName : ref;
                    beanDefinition.getPropertyValues().addPropertyValue("serviceUrl", url);
                    beanDefinition.getPropertyValues().addPropertyValue("serviceInterface", beanDefinition.getBeanClassName());
                    for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                        logger.info("add property [" + entry + "]");
                        beanDefinition.getPropertyValues().addPropertyValue(entry.getKey(), entry.getValue());
                    }
                    beanDefinition.setAutowireCandidate(true);
                    beanDefinition.setBeanClass(HessianProxyFactoryBean.class);
                    result.add(new BeanDefinitionHolder(beanDefinition, ref));
                    this.getRegistry().removeBeanDefinition(holder.getBeanName());
                    this.getRegistry().registerBeanDefinition(beanName, beanDefinition);
                    logger.info("beanName:" + beanName + ",serviceUrl:" + url);
                }
            } else {
                logger.error("there is no hessian customer");
            }
            logger.info("scan hessian customer interface end");
            return result;
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
        }
    }
}