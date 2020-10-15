package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.rsocket.metadata.WellKnownMimeType;

import java.nio.charset.StandardCharsets;

/**
 * RSocket Message mimetype metadata
 *
 * @author leijuan
 */
public class MessageMimeTypeMetadata implements MetadataAware {
    private byte mimeTypeId;
    private String mimeType;

    public MessageMimeTypeMetadata() {
    }

    public MessageMimeTypeMetadata(String mimeType) {
        this.mimeType = mimeType;
        try {
            WellKnownMimeType wellKnownMimeType = WellKnownMimeType.fromString(mimeType);
            this.mimeTypeId = wellKnownMimeType.getIdentifier();
        } catch (Exception ignore) {

        }
    }

    public MessageMimeTypeMetadata(WellKnownMimeType knownMimeType) {
        this.mimeTypeId = knownMimeType.getIdentifier();
        this.mimeType = knownMimeType.getString();
    }

    public MessageMimeTypeMetadata(RSocketMimeType rsocketMimeType) {
        this.mimeTypeId = rsocketMimeType.getId();
        this.mimeType = rsocketMimeType.getType();
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.MessageMimeType;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.MessageMimeType.getType();
    }

    @Override
    public ByteBuf getContent() {
        if (mimeTypeId > 0) {
            return Unpooled.wrappedBuffer(new byte[]{(byte) (mimeTypeId | 0x80)});
        } else {
            byte[] bytes = mimeType.getBytes(StandardCharsets.US_ASCII);
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(bytes.length + 1);
            buffer.writeByte(bytes.length);
            buffer.writeBytes(bytes);
            return buffer;
        }
    }

    @Override
    public void load(ByteBuf byteBuf) {
        byte firstByte = byteBuf.readByte();
        if (firstByte < 0) {
            this.mimeTypeId = (byte) (firstByte & 0x7F);
            this.mimeType = WellKnownMimeType.fromIdentifier(mimeTypeId).getString();
        } else {
            byteBuf.readCharSequence(firstByte, StandardCharsets.US_ASCII);
        }
    }

    public RSocketMimeType getRSocketMimeType() {
        return RSocketMimeType.valueOfType(this.mimeType);
    }

    public static MessageMimeTypeMetadata from(ByteBuf content) {
        MessageMimeTypeMetadata temp = new MessageMimeTypeMetadata();
        temp.load(content);
        return temp;
    }
}
