package github.starry.spring;

import github.starry.annotation.RpcAutoWired;
import github.starry.annotation.RpcService;
import github.starry.config.RpcServiceConfig;
import github.starry.extension.ExtensionLoader;
import github.starry.factory.SingletonFactory;
import github.starry.proxy.RpcClientProxy;
import github.starry.remoting.transport.RpcRequestTransport;
import github.starry.remoting.transport.netty.client.NettyRpcClient;
import github.starry.serviceprovider.ServiceProvider;
import github.starry.serviceprovider.impl.ZkServiceProviderImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @author Starry
 * @create 2023-01-28-8:25 PM
 * @Describe 该类实现BeanPostProcessor接口，可以在bean对象加入IOC容器前，完成两项工作。
 * 1.检查bean上有没有RpcService注解，如果有该注解，说明该类需要对外暴露，对外提供服务。
 * 因此需要将该服务发布到注册中心上。
 * 2.检查bean对象的Field上有没有RpcReference注解，如果有就说明该Field需要设置成代理类。
 */
@Component
public class SpringBeanPostProcessor implements BeanPostProcessor {

    private final ServiceProvider serviceProvider;

    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor() {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
        this.rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension("netty");
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        /*
            检查bean上有没有RpcService注解，如果有该注解，说明该类需要对外暴露，发布到注册中心,但不会改变bean对象。
         */
        RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
        if (rpcService == null) {
            return bean;
        }
        //对外暴露服务
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder().version(rpcService.version())
                .group(rpcService.group()).service(bean).build();
        //发布服务
        serviceProvider.publishService(rpcServiceConfig);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        /*
            检查bean对象的字段上有没有@RpcAutoWired注解，如果有则构建对应的动态代理对象，并通过反射，将该动态代理对象设置给bean的Field
         */
        //拿到bean的类型
        Class<?> clazz = bean.getClass();
        Field[] declaredFields = clazz.getDeclaredFields();
        //遍历类的所有字段
        for (Field field : declaredFields) {
            //如果该字段上存在注解
            if (field.isAnnotationPresent(RpcAutoWired.class)) {
                RpcAutoWired rpcAutoWired = field.getAnnotation(RpcAutoWired.class);
                RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder().
                        version(rpcAutoWired.version()).group(rpcAutoWired.group()).build();
                RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, rpcServiceConfig);
                Object proxyObject = rpcClientProxy.getProxy(field.getType());
                field.setAccessible(true);
                try {
                    //将bean对象的，带有RpcAutoWired注解的Field设为代理对象。
                    field.set(bean, proxyObject);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bean;
    }
}
