import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * hessian注解整合service注解
 *
 * @author wucc
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface HessianCustomer {
    /**
     * 提供者上下文
     */
    String provider();

    /**
     * 远程服务名称
     */
    String ref();

    /**
     * 实例名称
     */
    String value() default "";

    /**
     * 下面都是hessian配置，可以自行定义
     */
    int readTimeout() default 10000;

    boolean overloadEnabled() default false;
}
