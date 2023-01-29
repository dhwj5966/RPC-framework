package github.starry.remoting.message;

import github.starry.enums.RpcResponseCodeEnum;
import lombok.*;

import java.io.Serializable;

/**
 * @author Starry
 * @create 2023-01-22-1:20 PM
 * @Describe 服务提供者在接收到RpcRequest后，会对请求进行解析并进行方法调用，并根据调用结果封装出一个RpcResponse对象并发送给消费者。
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
@Setter
public class RpcResponse<T> implements Serializable {
    private static final long serialVersionUID = 1249816374234l;

    private String requestId;

    //响应码
    private Integer code;

    //响应附加信息
    private String message;

    //返回值
    private T data;

    //快速构造一个RpcResponse
    public static <T> RpcResponse<T> success(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(RpcResponseCodeEnum.SUCCESS.getCode());
        response.setMessage(RpcResponseCodeEnum.SUCCESS.getMessage());
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    public static <T> RpcResponse<T> fail(RpcResponseCodeEnum rpcResponseCodeEnum) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(rpcResponseCodeEnum.getCode());
        response.setMessage(rpcResponseCodeEnum.getMessage());
        return response;
    }


}
