package com.alibaba.rsocket.gateway.grpc;

import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.protobuf.GeneratedMessageV3;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Payload utils
 *
 * @author leijuan
 */
public class PayloadUtils {
    private static final Map<String, ByteBuf> compositeMetadataMap = new ConcurrentHashMap<>();
    private static final MessageMimeTypeMetadata protobufMetaEncoding = new MessageMimeTypeMetadata(RSocketMimeType.Protobuf);
    private static final LoadingCache<Class<?>, Method> parseFromMethodStore = Caffeine.newBuilder()
            .maximumSize(Integer.MAX_VALUE)
            .build(targetClass -> targetClass.getMethod("parseFrom", ByteBuffer.class));

    public static ByteBuf getCompositeMetadata(String serviceName, String methodName) {
        String key = serviceName + "." + methodName;
        if (!compositeMetadataMap.containsKey(key)) {
            GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata("", serviceName, methodName, "");
            RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(routingMetadata, protobufMetaEncoding);
            ByteBuf byteBuf = compositeMetadata.getContent();
            compositeMetadataMap.put(key, Unpooled.copiedBuffer(byteBuf));
            ReferenceCountUtil.release(byteBuf);
        }
        return compositeMetadataMap.get(key);
    }

    public static Payload constructRequestPayload(GeneratedMessageV3 request, String serviceName, String method) {
        return ByteBufPayload.create(Unpooled.wrappedBuffer(request.toByteArray()),
                PayloadUtils.getCompositeMetadata(serviceName, method).retainedDuplicate());
    }

    public static <T> T payloadToResponseObject(Payload payload, Class<T> responseClass) throws InvocationTargetException, IllegalAccessException {
        return (T) parseFromMethodStore.get(responseClass).invoke(null, payload.data().nioBuffer());
    }
}
