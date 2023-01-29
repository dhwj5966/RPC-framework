package github.starry.spring;


import github.starry.annotation.EnableRpcAnnotationScan;
import github.starry.annotation.RpcService;
import io.protostuff.Rpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.stereotype.Component;

@Slf4j
public class CustomScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {
    private static final String SPRING_BEAN_BASE_PACKAGE = "github.starry";
    private static final String BASE_PACKAGE_ATTRIBUTE_NAME = "basePackage";
    private ResourceLoader resourceLoader;

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /*
        扫描github.starry下@Component组件，加入到Spring的IOC容器中。
        以及主启动类下的@RpcService组件。
     */
    @Override
    public void registerBeanDefinitions
            (AnnotationMetadata annotationMetadata, BeanDefinitionRegistry beanDefinitionRegistry) {
        //1.获取EnableRpcAnnotationScan的packages的值。
        AnnotationAttributes rpcScanAnnotationAttributes =
                AnnotationAttributes.fromMap
                        (annotationMetadata.getAnnotationAttributes(EnableRpcAnnotationScan.class.getName()));
        String[] rpcScanBasePackages = new String[0];

        //在什么范围扫描rpcService注解。
        rpcScanBasePackages = new String[]
                {((StandardAnnotationMetadata) annotationMetadata).getIntrospectedClass().getPackage().getName()};

        // Scan the RpcService annotation
        CustomScanner rpcServiceScanner = new CustomScanner(beanDefinitionRegistry, RpcService.class);
        // Scan the Component annotation
        CustomScanner springBeanScanner = new CustomScanner(beanDefinitionRegistry, Component.class);
        if (resourceLoader != null) {
            rpcServiceScanner.setResourceLoader(resourceLoader);
            springBeanScanner.setResourceLoader(resourceLoader);
        }
        int springBeanAmount = springBeanScanner.scan(SPRING_BEAN_BASE_PACKAGE);
        log.info("springBeanScanner扫描的数量 [{}]", springBeanAmount);
        int rpcServiceCount = rpcServiceScanner.scan(rpcScanBasePackages);
        log.info("rpcServiceScanner扫描的数量 [{}]", rpcServiceCount);
    }

}
