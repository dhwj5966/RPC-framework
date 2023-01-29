package github.starry.annotation;

import java.lang.annotation.*;

/**
 * @author Starry
 * @create 2023-01-29-1:40 PM
 * @Describe 用于修饰Field，被该注解修饰的Field，会用动态代理类替代，动态代理类实现了远程调用的逻辑。
 */
@Documented
@Inherited
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcAutoWired {
    /**
     * 远程连接的服务的版本
     */
    String version() default "";

    /**
     * 根据group区分实现类
     */
    String group() default "";
}
