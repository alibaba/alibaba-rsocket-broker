RSocket Broker HTTP Gateway
===========================

### 调用规范

```
POST /com.alibaba.user.UserService/findById
Authorization: Bearer jwt_token
Content-Type: application/json
rsocket-timeout: 3S
rsocket-frame: 0x04

[
  1
]

```

### 约定

* 只接受application/json数据格式，也就是传入和传出都是json
* 安全验证目前只接受JWT

### Performance

* CompositeMetadata缓存: 根据path进行缓存，也就service name + method

### References

* https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-HTTP2.md