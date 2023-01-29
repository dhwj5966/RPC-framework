package github.starry.remoting.handler;

import github.starry.factory.SingletonFactory;
import github.starry.remoting.message.RpcRequest;
import github.starry.serviceprovider.ServiceProvider;
import github.starry.serviceprovider.impl.ZkServiceProviderImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Starry
 * @create 2023-01-28-3:43 PM
 * @Describe 当Server收到一个RpcRequest时，负责方法的实际调用。
 */
public class RpcRequestHandler {

    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        this.serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    /**
     * 根据rpcRequest进行方法调用。
     * @param rpcRequest
     * @return
     */
    public Object handle(RpcRequest rpcRequest) {
        try {
            Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
            Method method = service.getClass()
                    .getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            Object invoke = method.invoke(service, rpcRequest.getParamValues());
            return invoke;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
