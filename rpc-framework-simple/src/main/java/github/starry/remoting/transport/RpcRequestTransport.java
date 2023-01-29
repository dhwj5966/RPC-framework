package github.starry.remoting.transport;

import github.starry.extension.SPI;
import github.starry.remoting.message.RpcRequest;

/**
 * @author Starry
 * @create 2023-01-23-1:54 PM
 * @Describe send RpcRequest。
 */
@SPI
public interface RpcRequestTransport {
    /**
     * 发送rpcRequest给服务生产者。
     * @return CompletableFuture对象，收到生产者的回应后将结果放入该对象中。
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
