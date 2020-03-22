package com.alibaba.rsocket.cloudevents;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cloudevents.v1.CloudEventImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.ByteBufPayload;
import reactor.core.publisher.Mono;

import java.io.OutputStream;
import java.net.URI;

/**
 * RSocket with CloudEvents support
 *
 * @author leijuan
 */
public interface CloudEventRSocket extends RSocket {
    TypeReference<CloudEventImpl<ObjectNode>> CLOUD_EVENT_TYPE_REFERENCE = new TypeReference<CloudEventImpl<ObjectNode>>() {
    };

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
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
        try {
            ByteBufOutputStream bos = new ByteBufOutputStream(byteBuf);
            JsonUtils.objectMapper.writeValue((OutputStream) bos, cloudEvent);
            return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBuf);
        } catch (Exception e) {
            ReferenceCountUtil.safeRelease(byteBuf);
            throw new EncodingException(RsocketErrorCode.message("RST-700500", "CloudEventImpl", "ByteBuf"), e);
        }
    }
}
