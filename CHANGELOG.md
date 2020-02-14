Alibaba RSocket Broker变更记录
==========================

## 0.1.0-Alpha2

### 新特性

* Load Balance & Health Check
* Filter Chain
* Binary Routing
* Docker image & tgz
* Spring Boot Actuator
* Performance enhancement

## 0.1.0-Alpha1: 2019/12/12

### 新特性

* 基于gossip集群管理
* 基于Spring Cloud Config的配置配送
* 基于Spring Cloud Registry的服务发现
* 基于CloudEvents的事件推送模型
* TLS 1.3 & 1.2 for TCP
* Broker的Spring Boot Starter的支持
* RSocket Broker控制台

### bug修复

* 自动创建RSA Key Pair错误

### 接口调整

* BearerTokenMetadata调整为官方标准格式
* GSVRoutingMetadata结构进行调整，Method调整为tag，GSV为路由key
* Data MIME Type per stream规范实现

### 文档

* 基于Hugo创建站点

### 三方包更新

* Roaring Bitmap 0.8.12
* Netty 4.1.44
