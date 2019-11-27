package com.alibaba.account;

import com.google.protobuf.Int32Value;
import reactor.core.publisher.Mono;

/**
 * account reactive service with proto messages
 *
 * @author leijuan
 */
public interface AccountService {
    
    Mono<Account> findById(Int32Value id);
}
