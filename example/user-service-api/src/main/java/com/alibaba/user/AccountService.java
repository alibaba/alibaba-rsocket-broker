package com.alibaba.user;

import com.alibaba.rsocket.RSocketServiceInterface;
import com.google.protobuf.Int32Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * account reactive service with proto messages
 *
 * @author leijuan
 */
@RSocketServiceInterface
public interface AccountService {
    // Use protobuf for data encoding: if compile failed, please execute 'mvn compile' first to generate code from protobuf
    // 友情提示: 如果编译失败，请命令执行 mvn compile, 生成protobuf对应的Java代码。这里主要演示使用Protobuf进行对象序列化
    Mono<Account> findById(Int32Value id);

    Flux<Account> findByStatus(Int32Value status);

    Flux<Account> findByIdStream(Flux<Int32Value> idStream);
}
