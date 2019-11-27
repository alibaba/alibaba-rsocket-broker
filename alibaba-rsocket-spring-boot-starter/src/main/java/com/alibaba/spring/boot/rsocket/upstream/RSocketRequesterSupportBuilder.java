package com.alibaba.spring.boot.rsocket.upstream;

import com.alibaba.rsocket.RSocketRequesterSupport;
import io.rsocket.plugins.RSocketInterceptor;

/**
 * RSocket Requester support builder
 *
 * @author leijuan
 */
public interface RSocketRequesterSupportBuilder {

    RSocketRequesterSupportBuilder addResponderInterceptor(RSocketInterceptor interceptor);

    RSocketRequesterSupportBuilder addRequesterInterceptor(RSocketInterceptor interceptor);

    RSocketRequesterSupport build();
}
