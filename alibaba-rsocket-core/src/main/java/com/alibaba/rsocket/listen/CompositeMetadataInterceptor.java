package com.alibaba.rsocket.listen;

import io.rsocket.RSocket;
import io.rsocket.plugins.RSocketInterceptor;

/**
 * composite metadata parsing interceptor
 *
 * @author leijuan
 */
public class CompositeMetadataInterceptor implements RSocketInterceptor {
    private static CompositeMetadataInterceptor instance = new CompositeMetadataInterceptor();

    @Override
    public RSocket apply(RSocket rSocket) {
        return new CompositeMetadataRSocket(rSocket);
    }

    public static CompositeMetadataInterceptor getInstance() {
        return instance;
    }
}
