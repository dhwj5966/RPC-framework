package github.starry.registry;

import github.starry.extension.SPI;

import java.net.InetSocketAddress;

/**
 * @author Starry
 * @create 2023-01-24-5:47 PM
 * @Describe
 */
@SPI
public interface ServiceRegistry {
    /**
     * 服务注册。将服务名和该服务实例的ip + port存储到注册中心里。
     * @param rpcServiceName 服务名
     * @param inetSocketAddress ip + 端口号
     */
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);
}
