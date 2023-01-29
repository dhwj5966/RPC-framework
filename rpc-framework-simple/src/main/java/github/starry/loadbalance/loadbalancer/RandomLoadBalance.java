package github.starry.loadbalance.loadbalancer;

import github.starry.loadbalance.AbstractLoadBalance;
import github.starry.remoting.message.RpcRequest;

import java.util.List;
import java.util.Random;

/**
 * @author Starry
 * @create 2023-01-26-1:43 PM
 * @Describe 随机负载均衡
 */
public class RandomLoadBalance extends AbstractLoadBalance {
    private Random random = new Random();
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
