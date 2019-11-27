package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;

import static io.netty.buffer.Unpooled.buffer;

/**
 * Data Encoding metadata type
 *
 * @author leijuan
 */
public class DataEncodingMetadata implements MetadataAware {
    private static byte[] HESSIAN_DATATYPE = new byte[]{RSocketMimeType.Hessian.getId(), RSocketMimeType.Hessian.getId()};
    private static byte[] JSON_DATATYPE = new byte[]{RSocketMimeType.Json.getId(), RSocketMimeType.Json.getId()};
    private RSocketMimeType dataType;
    private RSocketMimeType acceptType;

    public DataEncodingMetadata() {
    }

    public DataEncodingMetadata(RSocketMimeType dataType) {
        this.dataType = dataType;
        this.acceptType = dataType;
    }

    public DataEncodingMetadata(RSocketMimeType dataType, RSocketMimeType acceptType) {
        this.dataType = dataType;
        this.acceptType = acceptType;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.DataEncoding;
    }

    public RSocketMimeType getDataType() {
        return dataType;
    }

    public void setDataType(RSocketMimeType dataType) {
        this.dataType = dataType;
    }

    @NotNull
    public RSocketMimeType getAcceptType() {
        return acceptType;
    }

    public DataEncodingMetadata returnType() {
        return new DataEncodingMetadata(acceptType);
    }


    @Override
    public String getMimeType() {
        return RSocketMimeType.DataEncoding.getType();
    }

    @Override
    public ByteBuf getContent() {
        if (acceptType == null) {
            ByteBuf buf = buffer(1);
            buf.writeByte(dataType.getId());
            return buf;
        } else {
            ByteBuf buf = buffer(2);
            buf.writeByte(dataType.getId());
            buf.writeByte(acceptType.getId());
            return buf;
        }
    }

    @Override
    public void load(ByteBuf byteBuf) {
        this.dataType = RSocketMimeType.valueOf(byteBuf.readByte());
        if (byteBuf.capacity() > 1) {
            this.acceptType = RSocketMimeType.valueOf(byteBuf.readByte());
        }
    }

    @Override
    public String toText() throws Exception {
        return dataType.getId() + ":" + acceptType.getId();
    }

    @Override
    public void load(String text) throws Exception {
        int sep = text.indexOf(":");
        if (sep > 0) {
            this.dataType = RSocketMimeType.valueOf(Byte.valueOf(text.substring(0, sep)));
            this.acceptType = RSocketMimeType.valueOf(Byte.valueOf(text.substring(sep + 1)));
        } else {
            this.dataType = RSocketMimeType.valueOf(Byte.valueOf(text.substring(0, sep)));
        }
    }

    public static ByteBuf hessianDataType() {
        return Unpooled.wrappedBuffer(HESSIAN_DATATYPE);
    }

    public static ByteBuf jsonDataType() {
        return Unpooled.wrappedBuffer(JSON_DATATYPE);
    }

    public static DataEncodingMetadata from(ByteBuf content) {
        DataEncodingMetadata temp = new DataEncodingMetadata();
        temp.load(content);
        return temp;
    }
}
