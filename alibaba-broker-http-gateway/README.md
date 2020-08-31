RSocket Broker HTTP Gateway
===========================

### 调用规范

```
POST /api/com.alibaba.user.UserService/findById
Authorization: Bearer jwt_token
Content-Type: application/json
rsocket-timeout: 3S
rsocket-frame: 0x04

[
  1
]

```

对应的curl测试命令如下：

```
$ curl -d '[1]' -H 'Content-Type: application/json' http://127.0.0.1:9998/api/com.alibaba.user.UserService/findById
```

### 约定

* 只接受application/json数据格式，也就是传入和传出都是json
* 安全验证目前只接受JWT

### 安全验证

RSocket HTTP Gateway默认不启动REST API安全验证，如果想启动REST API，请确保"~/.rsocket/jwt_rsa.pub"文件存在，这个机制和RSocket Broker一致。

请求发发起请求，需要提供对应的JWT Token， HTTP REST API许请求格式如下：

```
POST http://127.0.0.1:8282/com.alibaba.user.UserService/findById
Authorization: Bearer jwt_token
Content-Type: application/json

```
