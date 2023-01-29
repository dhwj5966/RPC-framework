package github.starry.loadbalance;

import github.starry.extension.SPI;
import github.starry.remoting.message.RpcRequest;

import java.util.List;

/**
 * @author Starry
 * @create 2023-01-26-12:42 PM
 * @Describe 负载均衡策略
 */
@SPI
public interface LoadBalance {

    /**
     * 根据提供服务的URL列表和要发送的rpcRequest信息，自动选择一个目标地址。
     * @param serviceUrlList 提供服务的ServiceUrl列表
     * @param rpcRequest 要发送的rpcRequest信息。
     * @return 目标服务地址,如果无法找到就返回null。
     */
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
