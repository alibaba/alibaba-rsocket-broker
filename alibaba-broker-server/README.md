Alibaba RSocket Broker Server
=============================
RSocket Broker Server，包含图形化控制台

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

RSocket Broker内置支持配置推送功能，基于KV存储

### 产品环境下的JWT验证

为了开发的方便，你可以在RSocket Broker Server的application.properties文件中添加以下配置，这样就可以取消JWT Token的验证。

```
rsocket.broker.auth-required=false

```

在产品环境下，RSocket Broker使用JWT RSA进行安全验证，你需要在用户根目录下的.rsocket子目录下放置一个jwt_rsa.pub的公钥文件，生成步骤如下：

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

### Gossip设置
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

##### 产品环境部署实践

* WebSocket开启: 如果有从外部网络接入的应用，建议Broker使用多端口监听，TCP + WebSocket，外部应用通过WebSocket接入，内部网络依然走TCP

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
