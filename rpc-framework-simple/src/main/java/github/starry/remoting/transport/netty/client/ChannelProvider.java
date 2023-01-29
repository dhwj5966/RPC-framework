package github.starry.remoting.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Starry
 * @create 2023-01-27-7:31 PM
 * @Describe Channel的缓存类
 */
@Slf4j
public class ChannelProvider {
    private final Map<String, Channel> cache = new ConcurrentHashMap<>();


    /**
     * 获取Channel，返回活跃的Channel，如果没有活跃的Channel则返回null。
     */
    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        if (cache.containsKey(key)) {
            Channel channel = cache.get(key);
            if (channel != null && channel.isActive()) {
                return channel;
            } else {
                cache.remove(key);
            }
        }
        return null;
    }

    /**
     * 向缓存中添加活跃Channel。
     */
    public void put(InetSocketAddress inetSocketAddress, Channel channel) {
        cache.put(inetSocketAddress.toString(), channel);
    }
}
