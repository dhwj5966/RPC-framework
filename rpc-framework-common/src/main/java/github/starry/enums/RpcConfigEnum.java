package github.starry.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Rpc配置文件支持的配置项。
 */
@AllArgsConstructor
@Getter
public enum RpcConfigEnum {

    RPC_CONFIG_PATH("rpc.properties"),
    ZK_ADDRESS("rpc.zookeeper.address");

    private final String propertyValue;

}
