humans.md
=========
humans.md和humans.txt的出发点是一样的，就是如何向其他人提供可以阅读的信息。
当应用向RSocket Broker注册时，我们希望应用能够给Broker提供一个可以给开发人员了解该应用的信息，这个就是humans.md
你可以理解为README.md，但是那个是给项目或者模块使用的，这个更精简些，主要是给其他开发人员或者运维同学使用的。


### 如何编写？

非常简单，在resources目录下创建一个humans.md就可以啦，样式如下，当然你可以自行发挥。


```markdown
# :rainbow: User Reactive Services support by RSocket :rainbow:

### TEAM

* 雷卷 - 核心工程师
  ---
  雷卷是项目的开发工程师，同时负责线上运维的工作
    - :email: Email: xxx@yyy.com
	- Dingtalk: 雷卷
	- Nick: 雷卷
	- :iphone: 186xxxxxx

### Thanks

* RSocket
  ---
  Application protocol providing Reactive Streams semantics
    - Site: http://rsocket.io
    - Twitter: [@RSocketIO](https://twitter.com/RSocketIO)


### App

* Name: Rsocket Server User Service
* Last update: 2019/11/21
* Language: English / Chinese
* Tech Stack: Java 8, Spring Boot, RSocket
* IDE: Intellij IDEA, Maven
```

这样在Broker控制台，我们就可以将这些信息展现给其他开发人员。

### References

* http://humanstxt.org/
