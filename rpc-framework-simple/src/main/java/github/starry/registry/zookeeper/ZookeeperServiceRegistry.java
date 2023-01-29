package github.starry.registry.zookeeper;

import github.starry.registry.ServiceRegistry;
import github.starry.registry.zookeeper.util.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

/**
 * @author Starry
 * @create 2023-01-24-9:36 PM
 * @Describe 用zookeeper的方式注册服务
 */
public class ZookeeperServiceRegistry implements ServiceRegistry {
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
