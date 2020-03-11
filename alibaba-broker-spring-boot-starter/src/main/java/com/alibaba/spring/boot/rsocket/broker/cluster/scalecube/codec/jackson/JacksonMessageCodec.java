package com.alibaba.spring.boot.rsocket.broker.cluster.scalecube.codec.jackson;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.cluster.transport.api.MessageCodec;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Jackson Message Codec for Message with customized ObjectMapper
 *
 * @author leijuan
 */
public class JacksonMessageCodec implements MessageCodec {
    private final ObjectMapper delegate;

    public JacksonMessageCodec() {
        this(DefaultObjectMapper.OBJECT_MAPPER);
    }

    public JacksonMessageCodec(ObjectMapper delegate) {
        this.delegate = delegate;
    }

    public Message deserialize(InputStream stream) throws Exception {
        return this.delegate.readValue(stream, Message.class);
    }

    public void serialize(Message message, OutputStream stream) throws Exception {
        this.delegate.writeValue(stream, message);
    }
}