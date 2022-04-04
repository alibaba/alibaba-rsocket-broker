package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.metadata.RSocketMimeType;
import org.jetbrains.annotations.NotNull;

/**
 * GraphQL json encoding - application/graphql+json
 *
 * @author leijuan
 */
public class ObjectEncodingHandlerGraphQLJsonImpl extends ObjectEncodingHandlerJsonImpl {
    @NotNull
    @Override
    public RSocketMimeType mimeType() {
        return RSocketMimeType.GraphQLJson;
    }
}
