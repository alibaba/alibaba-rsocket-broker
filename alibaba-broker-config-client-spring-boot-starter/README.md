RSocket Broker Config Client
============================
使用RSocket Broker作为配置中心，然后基于此中心对外提供配置推送服务。


### 如何使用？

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

### 工作原理

* 应用启动时使用HTTP短连接获取最新的配置。为何使用HTTP：简单，短连接。
* 应用启动后，通过metadataPush监听broker推送的cloud event(com.alibaba.rsocket.events.ConfigEvent)完成应用刷新
