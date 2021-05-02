RSocket Broker Config Client
============================
使用RSocket Broker作为配置推送的通讯桥梁，然后基于RSocket Broker对接入的应用提供配置推送服务。

### Config Server

RSocket Broker并不提供配置的存储服务，而是承担着配置推送通讯的桥梁角色，所以我们需要创建一个Config Server服务接入到RSocket Broker上。

我们提供了一个基于Redis KV存储和Stream实现的Config Server，请参考 https://github.com/alibaba-rsocket-broker/rsocket-broker-config-server-service

### 应用如何接入Config推送服务？

* 在pom.xml中添加以下依赖
```
<dependency>
    <groupId>com.alibaba.rsocket</groupId>
    <artifactId>alibaba-broker-config-client-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

* 在application.properties中添加以下配置，这三个配置项都是必须的

```
spring.application.name=your-app-name
rsocket.brokers=tcp://127.0.0.1:9999
rsocket.jwt-token=your_token_here
```

* 在Config Server中添加应用对应的配置项，如application.properties
* 启动应用进行测试

