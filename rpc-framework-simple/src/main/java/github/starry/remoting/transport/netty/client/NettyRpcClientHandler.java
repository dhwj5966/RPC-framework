package github.starry.remoting.transport.netty.client;

import github.starry.enums.CompressTypeEnum;
import github.starry.enums.SerializationTypeEnum;
import github.starry.factory.SingletonFactory;
import github.starry.remoting.constants.RpcConstants;
import github.starry.remoting.message.RpcMessage;
import github.starry.remoting.message.RpcResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author Starry
 * @create 2023-01-27-7:31 PM
 * @Describe 入栈处理器，负责处理入栈的RpcResponse信息，以及心跳数据。同时还负责发送心跳包。
 */
@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {

    private final UnprocessedRequests unprocessedRequests;
    private final NettyRpcClient nettyRpcClient;

    public NettyRpcClientHandler() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
        this.nettyRpcClient = SingletonFactory.getInstance(NettyRpcClient.class);
    }

    //收到消息的处理逻辑
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            log.error("msg is null");
        }
        if (msg instanceof RpcMessage) {
            RpcMessage message = (RpcMessage) msg;
            byte messageType = message.getMessageType();
            //如果收到的信息类型是心跳包的回复，那么记录一下日志就可以了
            if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                log.info("heart [{}]", message.getData());
            } else if (messageType == RpcConstants.RESPONSE_TYPE) {
                //如果收到的信息类型是RpcResponse
                RpcResponse<Object> rpcResponse = (RpcResponse<Object>) message.getData();
                unprocessedRequests.complete(rpcResponse);
            }
        }
        super.channelRead(ctx, msg);
    }

    //用户事件，发送心跳包
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //如果是一个IdleStateEvent事件
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            //如果是写事件
            if (state == IdleState.WRITER_IDLE) {
                log.info("write idle happen [{}]", ctx.channel().remoteAddress());
                //从ctx中获取ip + port，从而拿到channel。
                Channel channel = nettyRpcClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationTypeEnum.KRYO.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_REQUEST_TYPE);
                rpcMessage.setData(RpcConstants.PING);
                channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
