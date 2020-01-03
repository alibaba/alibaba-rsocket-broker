package com.alibaba.rsocket;

import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.DefaultPayload;

import static io.netty.buffer.Unpooled.buffer;

/**
 * Payload utils
 *
 * @author leijuan
 */
public class PayloadUtils {
    
    public static Payload cloudEventToPayload(CloudEventImpl<?> cloudEvent) {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(new MessageMimeTypeMetadata(RSocketMimeType.CloudEventsJson));
        return DefaultPayload.create(Unpooled.wrappedBuffer(Json.binaryEncode(cloudEvent)), compositeMetadata.getContent());
    }

    public static Payload cloudEventToMetadataPushPayload(CloudEventImpl<?> cloudEvent) {
        return DefaultPayload.create(Unpooled.EMPTY_BUFFER, Unpooled.wrappedBuffer(Json.binaryEncode(cloudEvent)));
    }

    /**
     * payload with data encoding if data encoding absent
     *
     * @param compositeMetadata composite metadata
     * @param payload           payload
     * @return payload
     */
    public static Payload payloadWithDataEncoding(RSocketCompositeMetadata compositeMetadata, MessageMimeTypeMetadata dataEncodingMetadata, Payload payload) {
        if (compositeMetadata.contains(RSocketMimeType.MessageMimeType)) {
            return payload;
        } else {
            ByteBuf buf = buffer(5);
            buf.writeByte((byte) (WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getIdentifier() | 0x80));
            buf.writeByte(0);
            buf.writeByte(0);
            buf.writeByte(1);
            buf.writeByte(dataEncodingMetadata.getRSocketMimeType().getId() | 0x80);
            CompositeByteBuf compositeByteBuf = new CompositeByteBuf(ByteBufAllocator.DEFAULT, true, 3, payload.metadata(), buf);
            return DefaultPayload.create(payload.data(), compositeByteBuf);
        }
    }
}
