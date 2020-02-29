Alibaba RSocket Broker
======================
[![Gitter](https://badges.gitter.im/alibaba-rsocket-broker/community.svg)](https://gitter.im/alibaba-rsocket-broker/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[!["Open Issues"](https://img.shields.io/github/issues-raw/alibaba/alibaba-rsocket-broker.svg)](https://github.com/alibaba/alibaba-rsocket-broker/issues)
[![Build Status](https://api.travis-ci.com/alibaba/alibaba-rsocket-broker.svg?branch=master)](https://travis-ci.com/alibaba/alibaba-rsocket-broker)
&nbsp;&nbsp;&nbsp;&nbsp;
[![English README](https://img.shields.io/badge/EN-README-brightgreen)](https://github.com/alibaba/alibaba-rsocket-broker/blob/master/README-en.md)

Alibaba RSocket Broker是一款基于RSocket协议的反应式程控消息交换系统，为通讯双方构建一个透明稳健的通讯桥梁。

* 反应式: 编程模型、线程模型、异步化、无网络要求等
* 程控：程序控制，可定制，如扩展支持等
* 消息：面向消息通讯，路由、过滤、observability都非常简单
* 交换系统：分布式、异构系统支持
* 更多RSocket架构方案，请访问 http://rsocketbyexample.info

### RSocket Broker工作原理
RSocket Broker是桥接应用间通讯的双方，相当于一个中间人的角色。
应用在启动后，和Broker创建一个长连接，在连接创建的时候需要标明自己的身份，如果是服务提供者，需要提交发布的服务信息。
Broker会针对所有的连接和服务列表建立对应的映射关系。
当一个应用需要调用其他服务时，应用会将请求以消息的方式发给Broker，然后Broker会解析消息的元信息，然后根据路由表将请求转发给服务提供者，然后将处理结果后的消息再转发给调用方。
Broker完全是异步化的，没有线程池这些概念，而且消息转发都是基于Zero Copy，所以性能非常高，这也是不用担心中心化Broker成为性能瓶颈的主要原因。

![RSocket Broker Structure](https://github.com/alibaba/alibaba-rsocket-broker/raw/master/alibaba-rsocket-broker-structure.png)

通过上述的架构图，RSocket Broker解决了传统设计中众多的问题：

* 配置推送: 通过RSocket的metadataPush可以完成配置推送
* 服务注册和发现：应用和Broker建立连接后，这个长连接就是服务注册和发现，你不需要额外的服务注册中心
* 透明路由: 应用在调用服务时，不需要知道服务对应的应用信息， Broker会完成路由
* Service-to-service调用: 就是RSocket的服务之间通讯的4个模型
* Load balancing: 应用和Broker建立长连接后，负载均衡就非常简单啦。
* Circuit Breakers: 断路保护，现在调整为Back Pressure支持
* Distributed messaging: RSocket就是基于消息推送的
* 多语言支持: RSocket是一套标准协议，主流语言的SDK都有支持，详情请访问 [RSocket SDK Stack](https://github.com/alibaba/alibaba-rsocket-broker/wiki/RSocket-SDK-Stack)

### 项目模块

* rsocket-service-common: RSocket服务接口定义基础模块，包括Annotation, Reactive相关框架和支撑类
* alibaba-rsocket-core: RSocket核心功能模块
* alibaba-rsocket-spring-boot-starter: Spring Boot Starter for RSocket, 包括RSocket服务发布和消费
* alibaba-broker-spring-boot-starter: Spring Boot Starter for RSocket Broker, 方便第三方进行扩展
* alibaba-rsocket-broker: Alibaba RSocket Broker参考实现
* alibaba-broker-registry-client-spring-boot-starter: 通过RSocket Broker对外提供服务发现服务
* alibaba-broker-config-client-spring-boot-starter: 通过RSocket Broker对外提供配置推送服务
* rsocket-broker-gateway-http: RSocket Broker HTTP网关，将HTTP转换为RSocket协议

### 开发环境要求

* JDK 1.8.0+
* Maven 3.5.x
* Node 10+: RSocket Broker采用Vaadin 14构建控制界面，所以你需要安装Node 10以上版本

### 如何运行Example?

**注意:** 样例代码中的AccountService接口采用了Protobuf进行序列化，使用了protobuf-maven-plugin生成对应的Protobuf，  
建议使用IDE导入项目之前，首先在项目的根目录下执行一下"mvn -DskipTests package"完成Protobuf对应的代码生成，不然直接在IDE中编译可能出现编译不通过的情况。

项目提供了完成的样例，你可以在[example模块](/example/)下找到，包括服务接口定义、服务实现和服务调用三个部分。

##### Docker Compose运行RSocket Broker

如果你本机已经安装了Docker和Docker Compose，建议直接运行 'docker-compose up -d' 启动RSocket Broker，当然你也可以手动运行RSocket Broker.

##### 手动运行RSocket Broker

* 找到AlibabaRSocketBrokerServer类，运行main函数，启动RSocket Broker

##### 运行 RSocket Responder & Requester

* 找到RSocketResponderServer类，运行main函数，启动RSocket Responder对外提供Reactive服务
* 找到RSocketRequesterApp类，运行main函数，启动RSocket Requester, 进行Reactive Service消费
* 在IDEA中，找到example.http，运行 "GET http://localhost:8181/user/2" 或者运行以下命令，进行服务调用测试。

```
$ curl http://localhost:8181/user/2
```

样例的详细介绍请访问 [Example](example)

### RSocket服务编写流程
包含如何创建一个Reactive服务接口，在Responder端实现该接口，在Requester完成Reactive服务调用。

* 创建一个RSocket服务接口，你可以创建一个单独的Maven Module存放这些接口，如user-service-api，样例代码如下：

```
public interface UserService {
    Mono<User> findById(Integer id);
}
```

* 在RSocket Responder端实现该接口，同时给实现类添加 @RSocketService annotation，如下：

```
@RSocketService(serviceInterface = UserService.class)
@Service
public class UserServiceImpl implements UserService {
    @Override
    public Mono<User> findById(Integer id) {
        return Mono.just(new User(1, "nick:" + id));
    }
}
```

不少开发者会问道，如果是MySQL数据库，如何和Reactive集成。目前R2DBC有对MySQL的支持，你可以参考一个Spring Cloud RSocket + R2DBC + MySQL的Demo实现: https://github.com/linux-china/spring-cloud-function-demo/

* 在RSocket Requester，以Proxy方式创建Reactive服务接口对应的Spring bean, 如下：

```
    @Bean
    public UserService userService(@Autowired UpstreamManager upstreamManager) {
        return RSocketRemoteServiceBuilder
                .client(UserService.class)
                .upstreamManager(upstreamManager)
                .build();
    }
```

* 在RSocket Requester端，进行代码调用，如HTTP REST API提供给:

```
@RestController
public class PortalController {
    @Autowired
    UserService userService;

    @GetMapping("/user/{id}")
    public Mono<User> user(@PathVariable Integer id) {
        return userService.findById(id);
    }
}
```


### References

* RSocket: http://rsocket.io/
* RSocket Java SDK: https://github.com/rsocket/rsocket-java
* Spring RSocket: https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#rsocket
* Spring Boot RSocket Starter: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-rsocket
* Project Reactor: http://projectreactor.io/
* Reactive Foundation: https://reactive.foundation/
