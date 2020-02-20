RSocket Broker Example
======================

requester <-> Broker <-> responder 三者通讯模型

# Maven Modules

* user-service-api: Reactive服务接口定义都在该模块中
* rsocket-responder: RSocket服务响应者，Reactive服务接口的实现和服务提供者
* rsocket-requester: RSocket服务请求者，调用远程的RSocket服务

# 服务启动

* 由于要使用到protobuf生成相应的代码和Vaadin(Node 10+要求)的编译，请先编译项目 mvn -DskipTests clean package
* 启动RSocket Broker: 控制台地址为 http://127.0.0.1:9998/
* 将JWT Token填写到rsocket-responder的application.properties，然后启动rsocket-responder, 和Broker建立长连接，完成鉴权、服务发布等
* 将JWT Token填写到rsocket-responder的application.properties, 然后启动rsocket-requester, 和Broker建立长连接，完成鉴权，然后进行服务调用

# 服务关闭

考虑到应用优雅下线的问题，我们建议采用以下步骤关闭应用。

```
$ curl -X POST http://localhost:8282/actuator/rsocket/shutdown
$ sleep 30s
$ curl -X POST http://localhost:8282/actuator/shutdown
```

其中8282端口是Spring Boot应用的管理功能端口号(management.server.port)

# 服务暂停

在实际的场景中，有可能需要Broker暂停向服务端发送信息，你可以通过控制台进行操作，当然你可以可以通过命令行操作应用。

```
$ curl -X POST http://localhost:8282/actuator/rsocket/offline
$ curl -X POST http://localhost:8282/actuator/online
```

# 测试

```
$ curl http://localhost:8181/user/2
```