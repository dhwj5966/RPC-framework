package github.starry.remoting.transport.netty.server;

import github.starry.config.CustomShutdownHook;
import github.starry.config.RpcServiceConfig;
import github.starry.factory.SingletonFactory;
import github.starry.remoting.transport.netty.codec.MessageCodec;
import github.starry.remoting.transport.netty.codec.RpcMessageLengthFieldBasedFrameDecoder;
import github.starry.serviceprovider.ServiceProvider;
import github.starry.serviceprovider.impl.ZkServiceProviderImpl;
import github.starry.utils.concurrent.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * @author Starry
 * @create 2023-01-28-2:02 PM
 * @Describe
 */
@Slf4j
@Component
public class NettyRpcServer {
    /**
     * 对外提供服务的端口号
     */
    public static final int port = 8899;

    //服务管理。
    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    /**
     * 向注册中心注册服务。
     */
    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    @SneakyThrows
    public void start() {
        //当JVM关闭的时候调用
        NioEventLoopGroup boss = null;
        NioEventLoopGroup workers = null;
        DefaultEventLoopGroup serviceHandlerGroup = new DefaultEventLoopGroup(
                Runtime.getRuntime().availableProcessors(),
                ThreadPoolFactoryUtil.createThreadFactory("serviceHandlerGroup", false)
        );
        try {
            CustomShutdownHook.getCustomShutdownHook().clearAll();
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            //boss-EventLoopGroup，只有一个线程，负责accept NioSocketChannel
            boss = new NioEventLoopGroup(1);
            //工作线程，负责读写等事件。
            workers = new NioEventLoopGroup();

            //在接收到数据后负责调用方法的线程。

            serverBootstrap = serverBootstrap.group(boss, workers).channel(NioServerSocketChannel.class);

            // nagle算法
            serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true)
            // 是否开启 TCP 底层心跳机制
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            //系统临时存放已完成三次握手的请求的队列的最大长度
            .option(ChannelOption.SO_BACKLOG, 128)
            .handler(new LoggingHandler(LogLevel.INFO));



            serverBootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    //5S读不到就触发事件
                    pipeline.addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
                    pipeline.addLast(new RpcMessageLengthFieldBasedFrameDecoder());
                    pipeline.addLast(new MessageCodec());
                    //具体的处理逻辑交由非NIO线程来做。
                    pipeline.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                }
            });
            String host = InetAddress.getLocalHost().getHostAddress();
            ChannelFuture future = serverBootstrap.bind(host, port).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("[{}]", e);
        } finally {
            boss.shutdownGracefully();
            workers.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }


}
