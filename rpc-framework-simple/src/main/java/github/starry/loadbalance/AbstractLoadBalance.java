package github.starry.loadbalance;

import github.starry.remoting.message.RpcRequest;

import java.util.List;

/**
 * @author Starry
 * @create 2023-01-26-1:39 PM
 * @Describe 负载均衡的抽象类，负载均衡的具体实现类需要继承该接口。
 */
public abstract class AbstractLoadBalance implements LoadBalance{
    @Override
    public String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest) {
        if (serviceUrlList == null || serviceUrlList.isEmpty()) {
            return null;
        }
        if (serviceUrlList.size() == 1) {
            return serviceUrlList.get(0);
        }
        return doSelect(serviceUrlList, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);
}
