package com.alibaba.rsocket.gateway.grpc;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.metadata.BinaryRoutingMetadata;
import com.alibaba.rsocket.metadata.GSVRoutingMetadata;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.utils.MurmurHash3;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.metadata.WellKnownMimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static com.alibaba.rsocket.reactive.ReactiveMethodSupport.getInferredClassForGeneric;

/**
 * Reactive gRPC method metadata
 *
 * @author leijuan
 */
public class ReactiveGrpcMethodMetadata {
    public static final Integer UNARY = 1;
    public static final Integer SERVER_STREAMING = 2;
    public static final Integer CLIENT_STREAMING = 3;
    public static final Integer BIDIRECTIONAL_STREAMING = 4;

    protected Class<?> inferredClassForReturn;
    private Class<?> returnType;
    private Integer rpcType;
    private String serviceName;
    private String name;
    private Integer serviceId;
    private Integer handlerId;
    private ByteBuf compositeMetadataByteBuf;

    public ReactiveGrpcMethodMetadata(Method method, String group, String serviceName, String version) {
        this.serviceName = serviceName;
        this.name = method.getName();
        this.returnType = method.getReturnType();
        this.inferredClassForReturn = getInferredClassForGeneric(method.getGenericReturnType());
        Class<?> parameterType = method.getParameterTypes()[0];
        if (parameterType.isAssignableFrom(Mono.class) && returnType.isAssignableFrom(Mono.class)) {
            this.rpcType = UNARY;
        } else if (parameterType.isAssignableFrom(Mono.class) && returnType.isAssignableFrom(Flux.class)) {
            this.rpcType = SERVER_STREAMING;
        } else if (parameterType.isAssignableFrom(Flux.class) && returnType.isAssignableFrom(Mono.class)) {
            this.rpcType = CLIENT_STREAMING;
        } else if (parameterType.isAssignableFrom(Flux.class) && returnType.isAssignableFrom(Flux.class)) {
            this.rpcType = BIDIRECTIONAL_STREAMING;
        }
        this.serviceId = MurmurHash3.hash32(ServiceLocator.serviceId(group, serviceName, version));
        this.handlerId = MurmurHash3.hash32(serviceName + "." + name);
        GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata(group, serviceName, this.name, version);
        //payload binary routing metadata
        BinaryRoutingMetadata binaryRoutingMetadata = new BinaryRoutingMetadata(this.serviceId, this.handlerId,
                routingMetadata.assembleRoutingKey().getBytes(StandardCharsets.UTF_8));
        //add param encoding
        MessageMimeTypeMetadata messageMimeTypeMetadata = new MessageMimeTypeMetadata(WellKnownMimeType.APPLICATION_PROTOBUF);
        RSocketCompositeMetadata compositeMetadata = RSocketCompositeMetadata.from(routingMetadata, messageMimeTypeMetadata);
        CompositeByteBuf compositeMetadataContent = (CompositeByteBuf) compositeMetadata.getContent();
        //add BinaryRoutingMetadata as first
        compositeMetadataContent.addComponent(true, 0, binaryRoutingMetadata.getHeaderAndContent());
        this.compositeMetadataByteBuf = Unpooled.copiedBuffer(compositeMetadataContent);
        ReferenceCountUtil.safeRelease(compositeMetadataContent);
    }

    public Class<?> getInferredClassForReturn() {
        return inferredClassForReturn;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Integer getRpcType() {
        return rpcType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getName() {
        return name;
    }

    public ByteBuf getCompositeMetadataByteBuf() {
        return compositeMetadataByteBuf;
    }
}
