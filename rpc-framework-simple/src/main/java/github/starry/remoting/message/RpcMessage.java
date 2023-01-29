package github.starry.remoting.message;

import lombok.*;

/**
 * @author Starry
 * @create 2023-01-27-12:32 AM
 * @Describe
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class RpcMessage {
    //消息类型
    private byte messageType;

    //序列化方式
    private byte codec;

    //压缩方式
    private byte compress;

    //请求序号
    private int requestId;

    //携带的具体数据，RpcRequest或RpcResponse
    private Object data;
}
