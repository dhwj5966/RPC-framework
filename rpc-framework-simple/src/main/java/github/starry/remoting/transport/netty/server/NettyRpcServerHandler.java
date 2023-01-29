package github.starry.remoting.transport.netty.server;

import github.starry.enums.CompressTypeEnum;
import github.starry.enums.RpcResponseCodeEnum;
import github.starry.enums.SerializationTypeEnum;
import github.starry.factory.SingletonFactory;
import github.starry.remoting.constants.RpcConstants;
import github.starry.remoting.handler.RpcRequestHandler;
import github.starry.remoting.message.RpcMessage;
import github.starry.remoting.message.RpcRequest;
import github.starry.remoting.message.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Starry
 * @create 2023-01-28-3:22 PM
 * @Describe Server的Handler，负责发送心跳包回应，收到RpcRequest时进行方法调用。
 */
@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {
    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    //读取
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof RpcMessage) {
                RpcMessage rpcMessage = (RpcMessage) msg;
                //分情况判断，如果是心跳包，返回一个RpcMessage。
                // 如果是RpcRequest，则进行方法调用。
                byte messageType = rpcMessage.getMessageType();
                //要发送给客户端的rpcMessage
                RpcMessage returnMessage = new RpcMessage();
                returnMessage.setCodec(SerializationTypeEnum.KRYO.getCode());
                returnMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
                    //如果是心跳包
                    rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    rpcMessage.setData(RpcConstants.PONG);
                } else {
                    //是RpcRequest包
                    RpcRequest rpcRequest = (RpcRequest) rpcMessage.getData();
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("server get result: %s", result.toString()));
                    //如果通道还在活跃且可以写入，则成功发送。
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        //快速构建rpcResponse
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        rpcMessage.setData(rpcResponse);
                    } else {
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        rpcMessage.setData(rpcResponse);
                        log.error("not writable now, message dropped");
                    }
                }
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            //需要手动释放msg。
            ReferenceCountUtil.release(msg);
        }
    }

    //需要对channel发生的异常进行捕获，因为异常不可避免，比如客户端异常地断开连接。
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server catch exception");
        cause.printStackTrace();
        ctx.close();
    }

    //当一定时间内没有收到,就关闭channel。
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                ctx.close();
                log.info("read event happened, close connection");
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
