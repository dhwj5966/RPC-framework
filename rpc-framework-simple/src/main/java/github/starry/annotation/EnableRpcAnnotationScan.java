package github.starry.annotation;

import github.starry.spring.CustomScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Starry
 * @create 2023-01-29-2:14 PM
 * @Describe 开启Rpc注解自动扫描，会自动扫描RpcService注解。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(CustomScannerRegistrar.class)
@Documented
public @interface EnableRpcAnnotationScan {

}
