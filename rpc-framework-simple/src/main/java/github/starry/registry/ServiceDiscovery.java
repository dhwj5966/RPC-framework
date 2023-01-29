package github.starry.registry;

import github.starry.extension.SPI;
import github.starry.remoting.message.RpcRequest;

import java.net.InetSocketAddress;

/**
 * @author Starry
 * @create 2023-01-24-5:46 PM
 * @Describe
 */
@SPI
public interface ServiceDiscovery {
    /**
     * 发现服务。
     * @param rpcRequest
     * @return
     */
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
