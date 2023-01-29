package github.starry.loadbalance.loadbalancer;


import github.starry.loadbalance.AbstractLoadBalance;

import github.starry.remoting.message.RpcRequest;
import lombok.extern.slf4j.Slf4j;


import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡。
 */
@Slf4j
public class PollLoadBalance extends AbstractLoadBalance {


    private AtomicInteger number = new AtomicInteger(0);

    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        int serverNumber = serviceAddresses.size();
        int value = number.getAndIncrement();
        int index = value % serverNumber;
        if (value > Integer.MAX_VALUE / 2) {
            number.set(0);
        }
        return serviceAddresses.get(index);
    }


}
