Alibaba RSocket Broker Server
=============================
RSocket Broker Server，主要包括RSocket Broker的核心功能和图形化控制台。

### JDK 要求

* Alibaba RSocket Broker兼容Java 8, 11、15和最新的16。 如果你使用Java 11、15和16，在运行时添加以下JVM参数。

```
 --illegal-access=permit -Dio.netty.tryReflectionSetAccessible=true --add-opens java.base/jdk.internal.misc=ALL-UNNAMED
```

如果Maven编译问题，请添加`MAVEN_OPTS=--illegal-access=permit` 和 JVM参数 `--illegal-access=permit` 然后进行编译。 

### 日常开发和测试

如果用于日常开发和测试，如果你已经使用Docker的话，你只需要创建一个对应的docker-compose.yml然后启动即可。

```yaml
version: "3"
services:
  alibaba-rsocket-broker:
    image: linuxchina/alibaba-rsocket-broker:1.0.1
    ports:
      - "9997:9997"
      - "9998:9998"
      - "9999:9999"
```

RSocket Broker的控制台地址为 http://localhost:9998/


### UI
RSocket Broker控制台默认采用Vaadin 14编写，主要是基于以下考虑：

* Vaadin特性比较丰富，而且比较简单，只要是Java程序员，就可以编写
* 扩展容易，如果你需要扩展，你需要添加一个view，或者扩展之前的Java Class就可以啦
* 目前采用最新的Vaadin 14版本，可以进行Web Component输出

##### 导航栏

* Dashboard:

    * top: service count, app count, connections etc, request count in last 1 minute
    * widgets: last 10 apps connected with broker
    * request/streams
    * channels

* Services: exposed service
* Apps: connected apps
* DNS: connected apps
* Broker: connected apps
* ServiceMesh: connected apps
* JWT: 生成RSocket应用连接到Broker时需要的JWT Token
* System: broker information, metrics, connections etc

### 配置推送

RSocket Broker内置支持配置推送功能，配置主要保存在内存中，我们强烈建议你使用适配如Consul, etc等，接下来我们会进行基于git仓库的配置对接。

### 产品环境下的JWT验证

为了开发的方便，你可以在RSocket Broker Server的application.properties文件中添加以下配置，这样就可以取消JWT Token的验证。

```
rsocket.broker.auth-required=false
```

或者在RSocket Broker Server启动命令行中添加取消JWT Token验证特性，命令行如下：

```
java -jar alibaba-broker-server/target/alibaba-rsocket-broker.jar --rsocket.broker.auth-required=false
```

在产品环境下，强力建议RSocket Broker使用JWT RSA进行安全验证，你需要在用户根目录下的.rsocket子目录下放置一个jwt_rsa.pub的公钥文件，生成步骤如下：

```
# generate a 2048-bit RSA private key
$ openssl genrsa -out jwt_private_key.pem 2048

# convert private Key to PKCS#8 format (so Java can read it)
$ openssl pkcs8 -topk8 -inform PEM -outform DER -in jwt_private_key.pem -out jwt_rsa.key -nocrypt

# output public key portion in DER format (so Java can read it)
$ openssl rsa -in jwt_private_key.pem -pubout -outform DER -out jwt_rsa.pub

```

如果你将jwt_rsa.key放置在~/.rsocket目录下，则RSocket Broker会帮助你生成JWT Token

如果你不做任何事情，RSocket Broker也会自动jwt_rsa.key 和 jwt_rsa.pub

如果你想自行控制JWT Token的生成，请参考 AuthenticationServiceJwtImpl 为应用生成token。

**注意:** 如果你使用Docker制作RSocket Broker镜像，如果是内部使用的话，你可以将jwt_rsa.pub合并到镜像也是可以的。

### TLS通讯加密
RSocket Broker 默认是不开启TLS的，如果你需要启动TLS，则需要为RSocket Broker生成一个key store文件，如下：

```
$ keytool -genkey -alias rsocket-broker -keyalg RSA –keysize 2048 -keypass changeit -storepass changeit -keystore rsocket.p12
$ cp rsocket.p12 ~/.rsocket/
```

然后将rsocket.p12文件拷贝到用户根目录的".rsocket" 子目录下，接下来在application.properties开启ssl就可以，如下：

```
rsocket.broker.ssl.enable=true
```

### Gossip广播设置和监听端口号42254
RSocket broker默认是开发者模式，也就是单机运行模式，如果你要开启基于Gossip广播的集群模式，请进行如下配置。

```
rsocket.broker.topology=gossip
rsocket.broker.seeds=192.168.1.2,192.168.1.3,192.168.1.4
```

基于Gossip广播，你只要需要设置一下种子节点列表就可以啦，然后再启动这些种子服务器就可以。Gossip的广播端口为42254，请确保该端口能够被访问。

### 产品环境部署
如果你在产品环境部署的话， 你需要注意以下一些事项：

* 开启JWT认证，给每一个应用分配特定的JWT token
* SSL开启选配，如果内部网络，可以考虑不开启，如果是从互联网外的应用对接，建议使用使用SSL
* 基于Gossip的集群管理，我们建议一个集群的最低配置为三台服务
* 优雅关闭Broker: 在关闭broker时，如broker程序更新，先调用 http://localhost:9998/ops/stop_local_broker 将该broker从集群摘除，然后再停止应用。我们建议在线上发布扩容时候，一台台进行，这样对SDK的通知简单不少。

### 产品环境网络拓扑结构支持

在某些情况下，RSocket Broker集群可能要为外部应用跨互联网提供接入支持，如跨不同的云厂商，也就是要同时支持intranet和internet接入支持。

##### intranet模式
所有的应用都可以访问broker实例的内部IP地址，Gossip广播的broker IP地址都可以被应用访问，这个也是最简单的。

*注意*: 如果外部应用跨互联网但是是VPN接入，这个是属于intranet范畴。

##### internet模式
应用从互联网接入，这个时候broker实例要以外部域名对外提供接入，这个时候需要broker包含对外的域名，你需要给每一个broker实例设置外部域名，如果是容器环境，你可以设置环境变量"RSOCKET_BROKER_EXTERNAL_DOMAIN"

```properties
rsocket.broker.external-domain=broker1.rsocket.foobar.com
#rsocket.broker.external-domain=ws://broker1.rsocket.foobar.com:8080/
```

如果你对外使用不同的端口和协议，如对互联网接入使用WebSocket，请将external-domain设置为URI方式

对于外部应用来说，在设置rsocket broker的互联网域名后，同时要将rsocket.topology设置为internet，如下:

```
rsocket.brokers=tcp://broker1.rsocket.foobar.com:9999,tcp://broker2.rsocket.foobar.com:9999
rsocket.topology=internet
```

处于安全的考虑，可以使用WebSocket代理方式进行,如Nginx 或者 [Babl](https://github.com/babl-ws/babl), 然后转发到RSocket Broker集群。

# 监听端口号
对于一个RSocket Broker集群来说，通常都会涉及到两个端口号，一个是接受请求端口号，在RSocket
Broker集群中为9999，另外一个是集群管理端口号，也就是集群内部服务器节点之间相互通讯的端口号,
RSocket的Gossip管理方式中，该端口号为42254，确保各个节点之间的gossip通讯。整体如下：

* tcp request port: 9999，负责接收RSocket请求
* cluster gossip port: 42254，完成集群内部服务器间的Gossip通讯
* Spring Boot management server port: 9997, Spring Boot的actuator端口号
* http web request port: 9998，通过该Web监听端口号，可以以REST API方式访问RSocket服务，同时是Broker集群的Web管理控制台。

如果你还要开启RSocket Broker的WebSocket端口监听，如为浏览器提供对应的接入，你只需要添加如下bean即可：

```
  @Bean
  public RSocketListenerCustomizer websocketListenerCustomizer() {
        return builder -> {
            builder.listen("ws", 19999);
        };
    }
```

### Kubernetes部署

* K8S准备工作: 主要是创建rsocket命名空间，同时为Spring-Cloud-Kubernetes访问K8S集群设置对应的权限 `kubectl apply -f alibaba-broker-server/src/main/k8s/setup.yml`
* 执行编译： mvn -Pk8s -DskipTests clean package
* K8S部署rsocket broker:  `kubectl apply --namespace=rsocket -f alibaba-broker-server/src/main/k8s/deployment.yml `
* 应用接入： application.properties中添加 `rsocket.brokers=tcp://rsocket-broker.rsocket.svc.cluster.local:9999`

### Vaadin Flow
Alibaba RSocket Broker的Web控制台使用Vaadin 14开发，为了方便你扩展界面，将Vaadin的开发资源列一下，方便二次开发。

* Vaadin App Layout: https://vaadin.com/tutorials/app-layout/vaadin  https://vaadin.com/tutorials/app-layout/appreciated
* Vaadin key concepts: https://vaadin.com/tutorials/vaadin-key-concepts
* Vaadin Platform: https://github.com/vaadin/platform
* Vaadin Flow Document: https://vaadin.com/docs/flow/Overview.html
* Vaadin Tutorials: https://vaadin.com/tutorials
* Reactive Chat App with Spring Boot, Project Reactor, and Vaadin: https://vaadin.com/tutorials/reactive-chat-app
* App layout: https://vaadin.com/components/vaadin-app-layout/java-examples  https://vaadin.com/api/com.vaadin/vaadin-app-layout-flow/1.0.3/com/vaadin/flow/component/applayout/package-summary.html
* Vaadin Icons: https://vaadin.com/components/vaadin-icons/java-examples
