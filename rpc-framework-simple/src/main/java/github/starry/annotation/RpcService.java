package github.starry.annotation;

import java.lang.annotation.*;

/**
 * @author Starry
 * @create 2023-01-29-1:28 PM
 * @Describe 用来修饰类，被该注解修饰的类会加入到IOC容器中，并对外暴露服务。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface RpcService {
    String version() default "";

    String group() default "";
}
