package com.alibaba.rsocket;

import com.alibaba.rsocket.metadata.DataEncodingMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import io.cloudevents.CloudEvent;
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
    public static Payload cloudEventToPayload(CloudEventImpl<?> cloudEvent) throws Exception {
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(new DataEncodingMetadata(RSocketMimeType.CloudEventsJson));
        return DefaultPayload.create(Unpooled.wrappedBuffer(Json.binaryEncode(cloudEvent)), compositeMetadata.getContent());
    }

    public static Payload cloudEventToMetadataPushPayload(CloudEventImpl<?> cloudEvent) throws Exception {
        return DefaultPayload.create(Unpooled.EMPTY_BUFFER, Unpooled.wrappedBuffer(Json.binaryEncode(cloudEvent)));
    }

    /**
     * payload with data encoding if
     *
     * @param compositeMetadata composite metadata
     * @param payload           payload
     * @return payload
     */
    public static Payload payloadWithDataEncoding(RSocketCompositeMetadata compositeMetadata, DataEncodingMetadata dataEncodingMetadata, Payload payload) {
        if (compositeMetadata.contains(RSocketMimeType.DataEncoding)) {
            return payload;
        } else {
            ByteBuf buf = buffer(6);
            buf.writeByte((byte) (WellKnownMimeType.MESSAGE_RSOCKET_DATA_ENCODING.getIdentifier() | 128));
            buf.writeByte(0);
            buf.writeByte(0);
            buf.writeByte(2);
            buf.writeByte(dataEncodingMetadata.getDataType().getId());
            buf.writeByte(dataEncodingMetadata.getAcceptType().getId());
            CompositeByteBuf compositeByteBuf = new CompositeByteBuf(ByteBufAllocator.DEFAULT, true, 3, payload.metadata(), buf);
            return DefaultPayload.create(payload.data(), compositeByteBuf);
        }
    }
}
