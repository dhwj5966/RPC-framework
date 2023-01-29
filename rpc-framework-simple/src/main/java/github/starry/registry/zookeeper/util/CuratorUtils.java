package github.starry.registry.zookeeper.util;

import github.starry.enums.RpcConfigEnum;
import github.starry.utils.PropertiesFileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Starry
 * @create 2023-01-25-12:12 AM
 * @Describe Zookeeper操作的工具类，利用Curator连接并操作zookeeper。
 */
@Slf4j
public class CuratorUtils {
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;

    //默认的zookeeper ip+port
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    private static CuratorFramework zkClient = null;
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();

    /**
     * 创建持久化节点。
     * @param zkClient 创建节点的客户端对象。
     * @param path 创建的节点的路径。
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                //eg: /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 获取指定rpcServiceName的子节点。
     * Gets the children under a node
     * @param zkClient
     * @param rpcServiceName
     * @return
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;

        try {
            //所需的结果就是Service node下的所有子节点的名字。
            result = zkClient.getChildren().forPath(servicePath);
            //取完值后更新一下Map,而且为该服务节点添加一个监听器。
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }

        return result;
    }

    /**
     * Empty the registry of data
     * @param zkClient
     * @param inetSocketAddress
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                    REGISTERED_PATH_SET.remove(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }

    public static CuratorFramework getZkClient() {
        // check if user has set zk address
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zookeeperAddress = properties != null &&
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null
                ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue())
                : DEFAULT_ZOOKEEPER_ADDRESS;
        // if zkClient has been started, return directly
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                // the server to connect to (can be a server list)
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        try {
            // wait 30s until connect to the zookeeper
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;
    }

    /**
     * 给指定服务所在的节点添加监听器。
     * 当指定节点发生变化的时候，更新SERVICE_ADDRESS_MAP中的数据。
     * @param rpcServiceName rpc服务名
     * @param zkClient
     * @throws Exception
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        //服务名在zookeeper中的路径。
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = new PathChildrenCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
                SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
            }
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }
}
