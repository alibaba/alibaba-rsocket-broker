Alibaba RSocket Broker变更记录
==========================

## 1.0.0-M3

### 新特

* Configuration调至为基于H2 MVStore，支持配置持久化到磁盘
* 升级到Vaadin 14.3.0，方便后续Modeless, resizable and draggable对话框，Spring Boot devtools支持
* ScaleCube Gossip更新至2.6.0，支持Jackson 2.11
* Sticky Session: 一个service的请求始终由某一服务提供者处理，不会做在多个服务提供者随机分发 https://github.com/alibaba/alibaba-rsocket-broker/wiki/RSocket-Routing

### Bug修复

* 修复Windows下WellKnownMimeType类重名bug
* 修改config推送应用名在broker上未设定的bug
* Spring Boot Devtools兼容测试

### 为何有WellKnownMimeType类重名问题

为何要创建一个重名的WellKnownMimeType类？ 这里主要是考虑MimeType扩展的问题。 RSocket Java SDK中，WellKnownMimeType是enum，
这表示非常难以扩展，但是在实际的企业内部，你可能还会使用到特定MimeType，如FlatBuffers，MessagePack等，但是这些都没有被WellKnownMimeType收纳，
我们希望通过同名类的覆盖机制，方便你自行添加对应的MimeType，方便你使用其他类型进行数据传输的序列化和反序列化。 如果有问题，欢迎反馈给我们。

## 1.0.0.M2

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M1

## 1.0.0M1

https://github.com/alibaba/alibaba-rsocket-broker/releases/tag/1.0.0.M1