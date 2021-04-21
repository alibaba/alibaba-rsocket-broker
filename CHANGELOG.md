Alibaba RSocket Broker变更记录
==========================

## 1.1.0

RSocket Broker 1.1将会基于RSocket Java 1.1.0和Spring Boot 2.4.x开发。

### 特性调整

* RSocket Java升级到1.1.0
* Spring Boot 2.4.4升级支持
* RSocket Broker Server支持Java 8，11和16
* RSocket Broker Server添加Testcontainers支持: https://github.com/alibaba-rsocket-broker/testcontainers-rsocket-broker-module
* Scalecube cluster升级至2.6.9：支持最新最新的Reactor Netty 1.0.x
* Docker镜像的基础镜像调整为adoptopenjdk:8-jdk-hotspot
* Kubernetes支持：快速部署RSocket Broker，应用接入便捷
* Graceful shutdown支持: Broker Server和应用均支持Graceful shutdown，在application.properties文件中添加`server.shutdown=graceful`即可


### 文档

* RSocket Broker测试支持： https://github.com/alibaba/alibaba-rsocket-broker/wiki/RSocket-Testing

### 样例

* RSocket Broker和Protobuf/gRPC开发支持： https://github.com/alibaba-rsocket-broker/rsocket-protobuf-service
* RSocket Broker和Kotlin开发支持： https://github.com/alibaba-rsocket-broker/alibaba-broker-kotlin-example
* 添加创建RSocket Broker应用模板: https://github.com/tgm-templates/rsocket-broker-example
* example模块添加user-service-spring-boot-starter，更方便第三方Spring Boot应用调用服务

## 1.0.1

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.1

## 1.0.0

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0

## 1.0.0.RC4

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.RC4

## 1.0.0.RC3

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.RC3

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