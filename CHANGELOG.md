Alibaba RSocket Broker变更记录
==========================

<!-- Keep a Changelog guide -> https://keepachangelog.com -->
<!-- Types of changes: Added, Changed, Deprecated, Removed, Fixed, Security, Document -->

## [1.1.1] - 2021-06-10

从1.1.1版本开始，我们决定对RSocket Broker进行瘦身，一味地叠加特性不是好的设计策略，而是将功能做的更可靠，提供更好的扩展才是好的策略。
所以接下来我们会所有的特性进行review，将其中一些不需要的特性进行删除，同时会增加一些基础特性，方便开发者在核心功能上进行扩展。

目前要移除的功能如下：

* Remove SMI beans：service meshes将调整到Kubernetes对接中 https://smi-spec.io/
* Config Server功能：从Broker中迁移到外部服务加载方式  https://github.com/alibaba-rsocket-broker/rsocket-broker-config-server-service
* HTTP DNS的功能移除

### 特性调整

* CloudEventsNotifyService： 可以给集群中任意应用或者任意应用实例列表发送CloudEvents事件
* 服务直连支持： 服务消费方通过Broker获取服务地址列表，然后直接给服务提供方通讯，Broker会提供对应的地址列表更新通知 https://github.com/alibaba/alibaba-rsocket-broker/wiki/RSocket-P2P
* CloudEvents Java SDK升级至2.1
* 添加对等通讯的能力，详细请参考： https://github.com/alibaba/alibaba-rsocket-broker/wiki/RSocket-P2P
* CloudEventImpl中添加事件来源，这样事件消费可以增加来源判断
* 添加GraphQL样例：https://github.com/alibaba-rsocket-broker/rsocket-graphql-gateway
* RSocket Broker控制台: Vaadin升级至20.0
* RSocket Java SDK升级至1.1.1
* Spring Boot 2.4.7/2.5.1兼容测试

## [1.1.0] - 2021-04-23

RSocket Broker 1.1将会基于RSocket Java 1.1.0和Spring Boot 2.4.x开发。

### 特性调整

* RSocket Java升级到1.1.0
* Spring Boot 2.4.4升级支持
* RSocket Broker Server支持Java 8，11和16
* RSocket Broker Server添加Testcontainers支持: https://github.com/alibaba-rsocket-broker/testcontainers-rsocket-broker-module
* Scalecube cluster升级至2.6.9：支持最新最新的Reactor Netty 1.0.x
* Docker镜像的基础镜像调整为adoptopenjdk:8-jdk-hotspot
* Kubernetes支持：快速部署RSocket Broker，同时应用接入Broker也更方便，已经比较稳定。
* Graceful shutdown支持: Broker Server和应用均支持Graceful shutdown，在application.properties文件中添加`server.shutdown=graceful`即可
* GraalVM native image兼容GraalVM 21.1.0
* Config Server推送添加了Redis的适配
* RSocket Broker集群变更推送添加幂等支持： 也就是应用接入端会每2分钟和Broker同步一次，拿取最新的Broker集群信息
* RSocket Broker Server的Docker镜像编译调整到Buildpacks，Paketo Buildpacks对Spring Boot支持更好

### 文档

* RSocket Broker测试支持： https://github.com/alibaba/alibaba-rsocket-broker/wiki/RSocket-Testing
* RSocket Broker Kubernetes: https://github.com/alibaba/alibaba-rsocket-broker/wiki/RSocket-Kubernetes

### 样例

* RSocket Broker和Protobuf/gRPC开发支持： https://github.com/alibaba-rsocket-broker/rsocket-protobuf-service
* RSocket Broker和Kotlin开发支持： https://github.com/alibaba-rsocket-broker/alibaba-broker-kotlin-example
* 添加创建RSocket Broker应用模板: https://github.com/tgm-templates/rsocket-broker-example
* example模块添加user-service-spring-boot-starter，更方便第三方Spring Boot应用调用服务

## [1.0.1] - 2021-03-23

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.1

## [1.0.0] - 2021-03-04

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0

## [1.0.0.RC4] - 2021-01-14

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.RC4

## [1.0.0.RC3] - 2020-11-12

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.RC3

## [1.0.0.RC2] - 2020-10-04

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.RC2

## [1.0.0.RC1] - 2020-08-31

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.RC1

## [1.0.0.M3] - 2020-07-24

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M3

## [1.0.0.M2] - 2020-05-17

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M2

## [1.0.0.M1] - 2020-03-22

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M1