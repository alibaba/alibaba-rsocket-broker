RSocket Broker gRPC Gateway
===========================
RSocket Broker的gRPC Gateway，你可以通过gRPC接口访问RSocket服务。

### 工作原理

工作原理非常简单，就是将gRPC请求转换为RSocket请求，中间的桥梁就是Reactive gRPC。具体的步骤如下：

* 编写proto文件
* 调用grpc和reactor-grpc plugin生成对应的Reactive服务接口
* 实现gRPC的Reactive服务接口，在接口实现中调用RSocket请求，完成交换

目前主要支持gRPC的RPC调用和streaming接口。

目前你还需要在接口实现中编写部分代码，完成RSocket调用。

### Testing Tools
你可以使用以下工具进行gRPC服务测试。

* evans: https://github.com/ktr0731/evans
* grpcurl: https://github.com/fullstorydev/grpcurl

### References

* Spring Boot starter module for gRPC framework: https://github.com/LogNet/grpc-spring-boot-starter
* Reactive stubs for gRPC: https://github.com/salesforce/reactive-grpc
* gRPC over HTTP2: https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md

