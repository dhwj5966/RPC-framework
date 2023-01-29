package github.starry.serviceprovider.impl;

import github.starry.config.RpcServiceConfig;
import github.starry.enums.RpcErrorMessageEnum;
import github.starry.exception.RpcException;
import github.starry.extension.ExtensionLoader;
import github.starry.registry.ServiceRegistry;
import github.starry.remoting.transport.netty.server.NettyRpcServer;
import github.starry.serviceprovider.ServiceProvider;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Starry
 * @create 2023-01-28-1:45 PM
 * @Describe
 */
@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {
    /**
     * rpcServiceName -> serviceInstance
     */
    private final Map<String, Object> serviceMap;

    /**
     * rpcServiceName = className + group +version
     */
    private final Set<String> registeredService;

    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        this.serviceMap = new ConcurrentHashMap<>();
        this.registeredService = ConcurrentHashMap.newKeySet();
        this.serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }

    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (serviceMap.containsKey(rpcServiceName)) return;
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        registeredService.add(rpcServiceName);
        log.info("Add service: {} and interfaces:{}",
                rpcServiceName,
                rpcServiceConfig.getService().getClass().getInterfaces());
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object o = serviceMap.get(rpcServiceName);
        if (o == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return o;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String rpcServiceName = rpcServiceConfig.getRpcServiceName();
            addService(rpcServiceConfig);
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            serviceRegistry.registerService(rpcServiceName, new InetSocketAddress(hostAddress, NettyRpcServer.port));
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
    }
}
