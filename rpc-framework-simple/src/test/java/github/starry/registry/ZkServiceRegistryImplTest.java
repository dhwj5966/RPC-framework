package github.starry.registry;


import github.starry.registry.zookeeper.util.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author shuang.kou
 * @createTime 2020年05月31日 16:25:00
 */
class ZkServiceRegistryImplTest {

    @Test
    void CuratorUtilsTest() {
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, "/my-rpc/tempup98/127.0.0.1:88");
        CuratorUtils.createPersistentNode(zkClient, "/my-rpc/tempup98/127.0.0.1:89");
        CuratorUtils.createPersistentNode(zkClient, "/my-rpc/tempup98/127.0.0.1:90");
        List<String> temp = CuratorUtils.getChildrenNodes(zkClient, "tempup98");
        assert temp.size() == 3;
        boolean flag = false;
        for (String s : temp) {
            if (s.equals("127.0.0.1:88")) {
                flag = true;
            }
        }
        assert flag;
        CuratorUtils.clearRegistry(zkClient, new InetSocketAddress("127.0.0.1", 88));
        CuratorUtils.clearRegistry(zkClient, new InetSocketAddress("127.0.0.1", 89));
        CuratorUtils.clearRegistry(zkClient, new InetSocketAddress("127.0.0.1", 90));
    }
}
