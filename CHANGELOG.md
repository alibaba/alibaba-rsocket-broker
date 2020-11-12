Alibaba RSocket Broker变更记录
==========================

## 1.0.0.RC3

#### 特性调整

* 添加Spring Cloud Function接入样例: https://github.com/alibaba/alibaba-rsocket-broker/blob/master/example/rsocket-cloud-function/README.md
* 增加连接过程中单个或多个服务上下线，如一些应用中，服务比较多，一些服务不可用时，可以做单个服务下线处理
* 调整RSocket多端口监听展现和元信息上报，可以同时展现RSocket监听的多端口信息
* 请求方的Remote IP获取和展现
* 添加flatbuffers & Cap’n Proto序列化框架类型声明
* RSocket Java 1.1.0和Spring Boot 2.4.0兼容: alibaba-rsocket-core和alibaba-rsocket-spring-boot-starter

#### Bug修复

* 修复server.port=0随机端口注册情况
* 修复Gossip广播时一致性hash的bug，感谢 @jimichan

#### 文档

* 增加RSocket Spring Boot actuator文档： https://github.com/alibaba/alibaba-rsocket-broker/wiki/Rsocket-Actuator
* 增加了RSocket Broker的服务规划： https://github.com/alibaba/alibaba-rsocket-broker/wiki/RSocket-Services

## 1.0.0.RC2

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.RC2

## 1.0.0.RC1

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.RC1

## 1.0.0.M3

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M3

## 1.0.0.M2

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M2

## 1.0.0.M1

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M1