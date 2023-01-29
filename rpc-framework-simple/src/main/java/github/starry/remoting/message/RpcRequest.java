package github.starry.remoting.message;

import lombok.*;

import java.io.Serializable;

/**
 * @author Starry
 * @create 2023-01-22-1:20 PM
 * @Describe 服务消费者向服务生产者发送的实体类。通过PrcRequest类，封装RPC所需的一切信息。
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest implements Serializable {

    //序列化所需的UID
    private static final long serialVersionUID = 12947652138547l;

    private String requestId;

    //调用的接口名
    private String interfaceName;

    //方法名
    private String methodName;

    //方法的参数类型
    private Class<?>[] paramTypes;

    //参数的值
    private Object[] paramValues;

    //服务版本
    private String version;

    //服务所在的组名
    private String group;

    /**
     * 该方法可以找出该request信息的服务名。
     * 实际存储在Zookeeper中的ServiceName是classname + group + version。
     * @return
     */
    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }


}
