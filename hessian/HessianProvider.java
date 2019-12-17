import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * hessian注解整合service注解
 *
 * @author wucc
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
public @interface HessianProvider {
    /**
     * 实例名称
     */
    String value() default "";

    /**
     * 下面都是hessian配置，可以自行定义
     */
    boolean allowNonSerializable() default true;
}
