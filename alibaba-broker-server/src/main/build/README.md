## Welcome to Alibaba RSocket Broker!

### What Is It?
Alibaba RSocket Broker is a broker server for RSocket messaging routing, such as RPC communication, Service Mesh, Streaming etc.  
Please visit  https://github.com/alibaba/alibaba-rsocket-broker for more.

### Requirements

* JDK 1.8.0+

### How to run the server?

* Start the broker:

```
./startup.sh
```

And you can visit http://127.0.0.1:9998/ to access RSocket Broker console.

* Shutdown the broker:

```
./shutdown.sh
```

### Custom configuration
Please check config/application.properties for application configuration and config/logback-spring for logging configuration.

### Get last version
For every major version there is one download from https://github.com/alibaba/alibaba-rsocket-broker/releases


