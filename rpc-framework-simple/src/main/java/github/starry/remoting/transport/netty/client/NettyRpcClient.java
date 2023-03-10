package github.starry.remoting.transport.netty.client;

import github.starry.enums.CompressTypeEnum;
import github.starry.enums.SerializationTypeEnum;
import github.starry.extension.ExtensionLoader;
import github.starry.factory.SingletonFactory;
import github.starry.registry.ServiceDiscovery;
import github.starry.remoting.constants.RpcConstants;
import github.starry.remoting.message.RpcMessage;
import github.starry.remoting.message.RpcRequest;
import github.starry.remoting.message.RpcResponse;
import github.starry.remoting.transport.RpcRequestTransport;
import github.starry.remoting.transport.netty.codec.MessageCodec;
import github.starry.remoting.transport.netty.codec.RpcMessageLengthFieldBasedFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.sql.Time;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Starry
 * @create 2023-01-27-7:31 PM
 * @Describe
 */
@Slf4j
public class NettyRpcClient implements RpcRequestTransport {

    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap bootstrap;

    private final ChannelProvider channelProvider;

    private final ServiceDiscovery serviceDiscovery;

    private final UnprocessedRequests unprocessedRequests;

    public NettyRpcClient() {
        eventLoopGroup = new NioEventLoopGroup();

        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class);

        bootstrap.handler(new LoggingHandler(LogLevel.INFO));
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

        bootstrap.handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                //2S????????????????????????????????????
                pipeline.addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                pipeline.addLast(new RpcMessageLengthFieldBasedFrameDecoder());
                pipeline.addLast(new MessageCodec());
                pipeline.addLast(new NettyRpcClientHandler());
            }
        });
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
        unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequests.class);
    }

    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("The client has connected [{}] successful!", inetSocketAddress.toString());
                    completableFuture.complete(future.channel());
                } else {
                    throw new IllegalStateException();
                }
            }
        });
        return completableFuture.get();
    }

    /**
     * ????????????????????????ip???channel???
     */
    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null || !channel.isActive()) {
            channel = doConnect(inetSocketAddress);
            channelProvider.put(inetSocketAddress, channel);
        }
        return channel;
    }


    public void close() {
        eventLoopGroup.shutdownGracefully();
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) {
        CompletableFuture<RpcResponse<Object>> future = new CompletableFuture<>();
        //???????????????????????????????????????
        InetSocketAddress targetAddress = serviceDiscovery.lookupService(rpcRequest);
        //??????????????????ip??????????????????channel???
        Channel channel = getChannel(targetAddress);
        if (channel.isActive()) {
            //??????rpcRequest????????????RpcMessage
            RpcMessage message = RpcMessage.builder()
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .codec(SerializationTypeEnum.KRYO.getCode())
                    .data(rpcRequest)
                    .build();

            //????????????????????????unprocessedRequests?????????requestId->future????????????
            unprocessedRequests.put(rpcRequest.getRequestId(), future);
            //???channel???????????????????????????????????????????????????????????????????????????
            channel.writeAndFlush(message).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future0) throws Exception {
                    //?????????????????????????????????????????????????????????
                    if (future0.isSuccess()) {
                        log.info("client send message: [{}]", message);
                    } else {
                        //????????????????????????,???????????????,????????????????????????future
                        future0.channel().close();
                        future.completeExceptionally(future0.cause());
                        log.error("Send failed:", future0.cause());
                    }
                }
            });
        } else {
            throw new IllegalStateException();
        }

        return future;
    }
}
