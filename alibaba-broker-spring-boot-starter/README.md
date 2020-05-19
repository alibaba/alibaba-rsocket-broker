RSocket Broker Spring Boot Starter
==================================
RSocket Broker的核心功能，以Spring Boot starter方式呈现，方便第三方进行功能扩展

### 特性

* 配置推送:  ConfigurationService & ConfigController
* 服务注册: rsocket-registry-client-spring-boot-starter
* 服务器: 端口监听，接受请求
* 服务调用路由
* 基于JWT的安全特性
* RSocket Broker集群管理: Gossip

### 扩展点

* 添加RSocket拦截器Interceptor
* 添加Ops REST API接口
* Metrics采集

### 配置推送 ConfigurationService

RSocket Broker内置配置推送功能，当前的配置采用本地文件存储，基于H2
MVStore的KV存储特性，默认的文件保存在"~/.rsocket/appsConfig.db"文件中。

当然你可以扩展自己的ConfigurationService，如对接etcd，Consul, ZooKeeper等。


