package github.starry.proxy;



import github.starry.config.RpcServiceConfig;
import github.starry.enums.RpcErrorMessageEnum;
import github.starry.enums.RpcResponseCodeEnum;
import github.starry.exception.RpcException;
import github.starry.extension.ExtensionLoader;
import github.starry.remoting.message.RpcRequest;
import github.starry.remoting.message.RpcResponse;
import github.starry.remoting.transport.RpcRequestTransport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author Starry
 * @create 2023-01-29-3:28 AM
 * @Describe 动态代理类，用于将SpringBean对象
 */
public class RpcClientProxy implements InvocationHandler {
    private static final String INTERFACE_NAME = "interfaceName";

    /**
     * 负责在网络中传输数据的Netty客户端。
     */
    private final RpcRequestTransport rpcRequestTransport;

    /**
     * 蕴含了该代理类所需指明的version和group，由调用者指定。
     */
    private final RpcServiceConfig rpcServiceConfig;

    /**
     * @param rpcRequestTransport 负责在网络中传输数据的客户端。
     * @param rpcServiceConfig 蕴含了该代理类所需指明的version和group，由调用者指定。
     */
    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig rpcServiceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = rpcServiceConfig;
    }

    /**
     * 获取动态代理类。传入一个接口。
     */
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    /**
     * 实际调用
     */
    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        RpcRequest rpcRequest = RpcRequest.builder()
                //requestId是随机的
                .requestId(UUID.randomUUID().toString())
                .paramTypes(method.getParameterTypes())
                .paramValues(objects)
                .methodName(method.getName())
                .interfaceName(method.getDeclaringClass().getName()).
                version(rpcServiceConfig.getVersion())
                .group(rpcServiceConfig.getGroup()).build();
        CompletableFuture<RpcResponse<Object>> future =
                (CompletableFuture<RpcResponse<Object>>) rpcRequestTransport.sendRpcRequest(rpcRequest);
        RpcResponse<Object> rpcResponse = future.get();

        if (rpcResponse == null) {
            throw new RpcException
                    (RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE,
                            INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException
                    (RpcErrorMessageEnum.REQUEST_NOT_MATCH_RESPONSE,
                            INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException
                    (RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE,
                            INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }

        return rpcResponse.getData();
    }
}
