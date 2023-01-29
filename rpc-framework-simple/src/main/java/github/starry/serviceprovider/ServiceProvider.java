package github.starry.serviceprovider;

import github.starry.config.RpcServiceConfig;

/**
 * @author Starry
 * @create 2023-01-28-1:41 PM
 * @Describe 服务的管理者，负责服务的添加，获取以及发布到注册中心。
 */
public interface ServiceProvider {
    /**
     * 添加服务。
     */
    void addService(RpcServiceConfig rpcServiceConfig);

    /**
     * 根据rpcServiceName，获取服务实例。如果不存在服务，则抛出异常。
     */
    Object getService(String rpcServiceName);

    /**
     * 发布服务到注册中心。
     */
    void publishService(RpcServiceConfig rpcServiceConfig);
}
