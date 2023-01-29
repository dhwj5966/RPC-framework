package github.starry.remoting.transport.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author Starry
 * @create 2023-01-27-12:10 AM
 * @Describe 处理粘包半包。
 * 协议格式：魔数-4B，version-1B，整个数据帧的长度-4B，消息类型-1B，压缩类型-1B，序列化类型-1B，requestId-4B，body。
 */
public class RpcMessageLengthFieldBasedFrameDecoder extends LengthFieldBasedFrameDecoder {
    public RpcMessageLengthFieldBasedFrameDecoder() {
        super(
                1024 * 2,
                5,
                4,
                -9,
                0
                );
    }
}
