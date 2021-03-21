Alibaba RSocket Broker变更记录
==========================

## 1.0.1

#### 特性调整

* 添加CloudEvents支持，你可以在函数结构中直接使用CloudEvent接口类型，代码如下：

```
    Mono<Void> fireLoginEvent(CloudEvent loginEvent);

    Mono<CloudEvent> processLoginEvent(CloudEvent loginEvent);
```

* 升级至Vaadin 19，完成JDK 11和15的兼容测试，RSocket Broker可以运行在JDK 11和15版本之上

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