RSocket Broker Registry Client
==============================
使用RSocket Broker作为服务注册中心，然后基于此中心对外提供registry client查询服务。


### 如何使用？

* 在pom.xml中添加以下依赖
```
<dependency>
    <groupId>com.alibaba.rsocket</groupId>
    <artifactId>alibaba-broker-registry-client-spring-boot-starter</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

* 在application.properties中添加

```
rsocket.brokers=tcp://127.0.0.1:9999
rsocket.jwt-token=token_here
```

* 在代码上添加enable discovery client

```
@SpringBootApplication
@EnableDiscoveryClient
public class RSocketRegistryClientApp
```

* 直接在代码中使用DiscoveryClient就可以啦

```
    @Autowired
    private DiscoveryClient discoveryClient;
```
