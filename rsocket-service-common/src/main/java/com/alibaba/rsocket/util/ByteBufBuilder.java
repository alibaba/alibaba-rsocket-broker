package com.alibaba.rsocket.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Byte buf builder
 *
 * @author leijuan
 */
public class ByteBufBuilder {
    ByteBuf buf = Unpooled.buffer();

    public static ByteBufBuilder builder() {
        return new ByteBufBuilder();
    }

    public ByteBufBuilder value(byte value) {
        buf.writeByte(value);
        return this;
    }

    public ByteBufBuilder value(int value) {
        buf.writeInt(value);
        return this;
    }

    public ByteBufBuilder value(long value) {
        buf.writeLong(value);
        return this;
    }

    public ByteBufBuilder value(double value) {
        buf.writeDouble(value);
        return this;
    }

    public ByteBufBuilder value(boolean value) {
        buf.writeByte(value ? 1 : 0);
        return this;
    }

    public ByteBufBuilder value(byte[] value) {
        buf.writeInt(value.length);
        buf.writeBytes(value);
        return this;
    }

    public ByteBufBuilder value(ByteBuf value) {
        buf.writeInt(value.readableBytes());
        buf.writeBytes(value);
        return this;
    }

    public ByteBufBuilder value(String value) {
        if (value == null || value.isEmpty()) {
            buf.writeInt(0);
        } else {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
        }
        return this;
    }

    public ByteBufBuilder value(Date value) {
        buf.writeLong(value.getTime());
        return this;
    }

    public ByteBuf build() {
        return this.buf;
    }

}
