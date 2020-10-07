Spring Cloud Function RSocket Demo
==================================

# 函数命名规范
函数的命名格式为: package名称 + 分组名称(首字母大写) + 函数名，如 `com.example.WordService.uppercase`，具体解释如下：

* package名称: 如com.example, com.example.user，采用Java的package命名规范，可以细到产品线、产品、部门等层面。
* 分组名称: 将函数归为某一分组，如WordService、UserService等，首字符大写，将函数进行一个大致归类，这个主要方便路由。另外要确保同一package下不要重新同名分组。
* 函数名称: 如uppercase，这个属于具体的函数名称，有表达其功能的含义。

有了标准的函数命名规范后，也方便函数向RSocket Broker注册和请求路由。

# Reference

* Spring Cloud Function Docs: https://docs.spring.io/spring-cloud-function/docs/3.0.10.RELEASE/reference/html/