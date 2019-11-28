RSocket Broker Example
======================

requester <-> Broker <-> responder 三者通讯模型

# Maven Modules

* user-service-api: Reactive服务接口定义都在该模块中
* rsocket-responder: RSocket服务响应者，Reactive服务接口的实现和服务提供者
* rsocket-requester: RSocket服务请求者，调用远程的RSocket服务

# 服务启动

* 启动RSocket Broker: 控制台地址为 http://127.0.0.1:9998/
* 将JWT Token填写到rsocket-responder的application.properties，然后启动rsocket-responder, 和Broker建立长连接，完成鉴权、服务发布等
* 将JWT Token填写到rsocket-responder的application.properties, 然后启动rsocket-requester, 和Broker建立长连接，完成鉴权，然后进行服务调用

# 测试

```
$ curl http://localhost:8181/user/2
```