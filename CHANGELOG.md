Alibaba RSocket Broker变更记录
==========================

## 1.0.0-M2

### 新特性

* Zipkin Tracing支持
* Prometheus Metrics集群化采集
* Kotlin Coroutines支持，你可以使用suspend method和Flow接口
* Kotlin serialization支持
* RSocket Cluster: 支持Internet和Intranet接入，同时支持Cluster节点之间的jsonrpc调用
* IoT接入优化: 支持基于一致性Hash算法的单连接接入
* gRPC优化： 支持全类型gRPC通讯方式

### 内部调整

* RSocket Java 1.0.0发布啦 :rose:
* ByteBuddy替换JDK Proxy，性能提升
* 添加Origin Metadata，服务提供方可以知道了解调用方的信息

### 文档

* 基于JWT的安全验证的详细文档说明
* Circuit Break和Back Pressure区别
* 可观测性文档完善

## 1.0.0-M1

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M1