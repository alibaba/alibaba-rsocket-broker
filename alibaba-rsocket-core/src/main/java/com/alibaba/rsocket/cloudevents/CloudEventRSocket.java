package com.alibaba.rsocket.cloudevents;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.ByteBufPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * RSocket with CloudEvents support
 *
 * @author leijuan
 */
public interface CloudEventRSocket extends RSocket {
    Mono<Void> fireCloudEvent(CloudEventImpl<?> cloudEvent);

    Mono<Void> fireEventReply(URI replayTo, EventReply eventReply);

    default Payload constructEventReplyPayload(URI replyTo, EventReply eventReply) {
        String path = replyTo.getPath();
        String serviceName = path.substring(path.lastIndexOf("/") + 1);
        String method = replyTo.getFragment();
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(new GSVRoutingMetadata("", serviceName, method, ""), new MessageMimeTypeMetadata(WellKnownMimeType.APPLICATION_JSON));
        return ByteBufPayload.create(JsonUtils.toJsonByteBuf(eventReply), compositeMetadata.getContent());
    }

    default Payload cloudEventToMetadataPushPayload(CloudEventImpl<?> cloudEvent) {
        try {
            return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, Unpooled.wrappedBuffer(Json.serialize(cloudEvent)));
        } catch (Exception e) {
            throw new EncodingException(RsocketErrorCode.message("RST-700500", "CloudEventImpl", "ByteBuf"), e);
        }
    }

    @Nullable
    default CloudEventImpl<JsonNode> extractCloudEventsFromMetadataPush(@NotNull Payload payload) {
        String jsonText = null;
        byte firstByte = payload.metadata().getByte(0);
        // json text: well known type > 127, and normal mime type's length < 127
        if (firstByte == '{') {
            jsonText = payload.getMetadataUtf8();
        } else {  //composite metadata
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(payload.metadata());
            if (compositeMetadata.contains(RSocketMimeType.CloudEventsJson)) {
                jsonText = compositeMetadata.getMetadata(RSocketMimeType.CloudEventsJson).toString(StandardCharsets.UTF_8);
            }
        }
        if (jsonText != null) {
            return Json.decodeValue(jsonText);
        }
        return null;
    }

}
