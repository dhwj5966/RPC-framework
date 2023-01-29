package github.starry.config;

import lombok.*;

/**
 * @author Starry
 * @create 2023-01-28-1:39 PM
 * @Describe 服务的抽象。
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcServiceConfig {
    //version of service
    private String version = "";

    //当接口有多个实现类，用group区分
    private String group = "";

    //服务
    private Object service;

    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }

    public String getServiceName() {
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }
}
