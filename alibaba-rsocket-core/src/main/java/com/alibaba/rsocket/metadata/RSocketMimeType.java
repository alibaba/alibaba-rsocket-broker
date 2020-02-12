package com.alibaba.rsocket.metadata;

import io.rsocket.metadata.WellKnownMimeType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RSocket mime type
 *
 * @author leijuan
 */
public enum RSocketMimeType {
    Json("Json", WellKnownMimeType.APPLICATION_JSON),
    Protobuf("Protobuf", WellKnownMimeType.APPLICATION_PROTOBUF),
    Avor("Avor", WellKnownMimeType.APPLICATION_AVRO),
    Hessian("Hessian", WellKnownMimeType.APPLICATION_HESSIAN),
    Text("Text", WellKnownMimeType.TEXT_PLAIN),
    Binary("Binary", WellKnownMimeType.APPLICATION_OCTET_STREAM),
    Java_Object("JavaObject", WellKnownMimeType.APPLICATION_JAVA_OBJECT),
    CBOR("CBOR", WellKnownMimeType.APPLICATION_CBOR),
    CloudEventsJson("CloudEventsJson", WellKnownMimeType.APPLICATION_CLOUDEVENTS_JSON),
    Application("Meta-Application", WellKnownMimeType.MESSAGE_RSOCKET_APPLICATION),
    CacheControl("Meta-CacheControl", WellKnownMimeType.MESSAGE_RSOCKET_DATA_CACHE_CONTROL),
    ServiceRegistry("Meta-Service-Registry", WellKnownMimeType.MESSAGE_RSOCKET_SERVICE_REGISTRY),
    BearerToken("Meta-BearerToken", WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION),
    Tracing("Meta-Tracing", WellKnownMimeType.MESSAGE_RSOCKET_TRACING_ZIPKIN),
    Routing("Meta-Routing", WellKnownMimeType.MESSAGE_RSOCKET_ROUTING),
    BinaryRouting("Meta-BinaryRouting", WellKnownMimeType.MESSAGE_RSOCKET_BINARY_ROUTING),
    MessageMimeType("Message-MimeType", WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE),
    MessageAcceptMimeTypes("Message-Accept-MimeTypes", WellKnownMimeType.MESSAGE_RSOCKET_ACCEPT_MIMETYPES),
    CompositeMetadata("Meta-Composite", WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA),
    MessageTags("Message-Tags", WellKnownMimeType.MESSAGE_RSOCKET_MESSAGE_TAGS),
    MessageOrigin("Message-Origin", WellKnownMimeType.MESSAGE_RSOCKET_MESSAGE_ORIGIN);

    public static final Map<Byte, RSocketMimeType> MIME_TYPE_MAP;
    public static final Map<String, RSocketMimeType> MIME_MIME_MAP;

    static {
        MIME_TYPE_MAP = Stream.of(RSocketMimeType.values()).collect(
                Collectors.toMap(RSocketMimeType::getId, x -> x));
        MIME_MIME_MAP = Stream.of(RSocketMimeType.values()).collect(
                Collectors.toMap(RSocketMimeType::getType, x -> x));
    }

    private byte id;
    private String name;
    private String type;

    RSocketMimeType(byte id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    RSocketMimeType(String name, WellKnownMimeType type) {
        this.id = type.getIdentifier();
        this.type = type.getString();
        this.name = name;
    }

    public byte getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
    
    public static RSocketMimeType valueOf(byte id) {
        return MIME_TYPE_MAP.get(id);
    }

    @Nullable
    public static RSocketMimeType valueOfType(String type) {
        if (type == null) return null;
        return MIME_MIME_MAP.get(type);
    }
}
