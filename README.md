# RPC-framework

## 介绍
RPC-framework 是一款基于Spring+Netty+Kryo+Zookeeper RPC框架。

## 快速启动
启动环境：maven + zookeeper

1.clone代码到本地

2.分别在rpc-framework-common和rpc-framework-simple文件夹下执行 
>mvn install

将项目打包并上传到maven仓库

3.在本地2181端口启动Zookeeper，作为服务注册中心，如果需要修改端口号,则在resources路径下创建rpc.properties文件，通过rpc.zookeeper.address=ip:port的方式配置。

4.分别创建生产者和消费者的SpringBoot项目，并且这两个项目都依赖commonAPI项目，commonAPI定义了接口HelloService。
```shell
//定义在commonAPI项目中的接口,生产者和消费者项目依赖commonAPI项目。
public interface HelloService {
    String sayHello(String name);
}
```
生产者和消费者都需要依赖打包的项目。
```shell
<dependency>
    <artifactId>rpc-framework-simple</artifactId>
    <groupId>com.starry</groupId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```


5.生产者的启动。

生产者提供HelloService的实现类。并通过@RpcService注解修饰，即可对外暴露服务。
```shell
@RpcService(version = "1.0", group = "1.0")
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return name + " say hello!";
    }
}
```
此外生产者需要启动Netty服务器，在主启动类所在包或所在包的子包中定义一个类实现如下代码即可。
```shell
//该类的作用是在SpringBoot项目启动后,持续运行NettyServer。
@Component
public class ServerStart implements ApplicationRunner {
    @Autowired
    NettyRpcServer nettyRpcServer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        nettyRpcServer.start();
    }
}
```
在主启动类上添加@EnableRpcAnnotationScan注解，开启注解扫描(识别Rpc相关注解),然后运行主启动类，即可对外提供服务，Spring会自动扫描主启动类所在包及其子包，
对于所有用@RpcSercice注解修饰的类，都会注册到注册中心中，对外暴露。
```shell
@SpringBootApplication
@EnableRpcAnnotationScan
public class RpcServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RpcServerApplication.class, args);
    }
}
```

6.消费者的启动

同样是在主启动类上添加@EnableRpcAnnotationScan注解，开启注解扫描。
```shell
@SpringBootApplication
@EnableRpcAnnotationScan
public class RpcClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcClientApplication.class, args);
    }

}
```
被@RpcAutoWired注解修饰的类,会被动态代理类代替，实现远程调用。
```shell
@RestController
public class HelloController {
    @RpcAutoWired(version = "1.0", group = "1.0")
    private HelloService helloService;

    @GetMapping("/hello")
    public String getName() {
        return helloService.sayHello("test");
    }

}
```
接下来运行客户端，访问getName()方法即可进行远程调用。

# 实现与原理
该RPC框架使用的主要技术包括：Spring + Netty + Kryo + Zookeeper

1.**注册中心**: 相关代码在github.starry.registry包下

服务注册:服务端启动时，会进行注解扫描，所有被@RpcService注解修饰的类， 根据其类名和@RpcService注解中的version和group字段值，
拼接出服务名，将该服务名及服务端的ip + port注册到Zookeeper。

服务发现:消费者服务中，被@RpcAutoWired注解修饰的类，被调用时就会进行远程调用，远程调用的一个重要步骤就是服务发现，
根据需要远程调用的类名，和@RpcAutoWired注解中的version和group字段值，拼接出服务名，从注册中心中读出ip + port。

2.**负载均衡**: 相关代码在github.starry.loadbalance包下

由于消费者在服务发现时可能发现有多个生产者端对外提供服务，因此需要有一定的策略在多个ip + port中选取一个，进行实际的远程调用。

3.**网络传输**: 相关代码在github.starry.remoting包下

远程调用需要在两台主机间发送数据，本项目采用基于NIO的Netty框架。

message包下抽象了RpcMessage、RpcRequest和RpcResponse类。

RpcRequest封装了消费者在远程调用时需要向生产者发送的信息，

RpcResponse则是生产者在接收到消费者的请求后，进行方法调用后，对方法调用结果的封装。

在实际的网络传输中，还需要一些其他的附加信息，如序列化方式，版本号等等，因此无论是RpcRequest还是RpcResponse，都被再封装到RpcMessage类中。

4.**序列化** 相关代码在github.starry.serialize包下

既然涉及到网络传输就一定涉及到序列化，本项目采用Kryo作为序列化方案。

5.**Spring** 相关代码在github.starry.spring包下

通过Spring以及动态代理，实现只需注解修饰，即可对外暴露服务或是远程调用。

SpringBeanPostProcessor类实现了BeanPostProcessor接口，在bean对象初始化过程中完成两项工作。
* 判断bean对象上有没有@RpcService注解，如果有该注解，说明该类需要对外暴露，对外提供服务。 因此需要将该服务发布到注册中心上。
* 遍历bean对象的所有字段，如果字段上有RpcReference注解，如果有就说明该字段需要设置为动态代理类。








