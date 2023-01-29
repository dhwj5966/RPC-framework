package github.starry.extension;

import java.lang.annotation.*;

/**
 * @author Starry
 * @create 2023-01-23-1:57 PM
 * @Describe 如果一个接口有多种实现类，将该接口视为一种规范，实现类需要完成对该接口的实现，
 * 可能有多种不同的实现方式。如果想通过ExtensionLoader获取该接口的实现类，就要用SPI注解修饰该接口。
 * ExtensionLoader会对接口的注解进行扫描，需要有SPI注解。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPI {
}
