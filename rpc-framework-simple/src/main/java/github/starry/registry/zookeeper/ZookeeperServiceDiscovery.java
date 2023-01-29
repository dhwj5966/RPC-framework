package github.starry.registry.zookeeper;

import github.starry.enums.RpcErrorMessageEnum;
import github.starry.exception.RpcException;
import github.starry.extension.ExtensionLoader;
import github.starry.loadbalance.LoadBalance;
import github.starry.registry.ServiceDiscovery;
import github.starry.registry.zookeeper.util.CuratorUtils;
import github.starry.remoting.message.RpcRequest;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author Starry
 * @create 2023-01-24-9:36 PM
 * @Describe 用zookeeper的方式发现服务。
 */
public class ZookeeperServiceDiscovery implements ServiceDiscovery {
    private LoadBalance loadBalance;

    public ZookeeperServiceDiscovery() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        String rpcServiceName = rpcRequest.getRpcServiceName();
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (serviceUrlList == null || serviceUrlList.isEmpty()) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        String serviceAddress = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        if (serviceAddress == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        String[] split = serviceAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }
}
