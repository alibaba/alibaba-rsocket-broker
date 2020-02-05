package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.CompositeMetadataFlyweight;
import io.rsocket.metadata.WellKnownMimeType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.rsocket.metadata.WellKnownMimeType.UNPARSEABLE_MIME_TYPE;

/**
 * RSocket composite metadata to wrap CompositeMetadata
 *
 * @author leijuan
 */
public class RSocketCompositeMetadata implements MetadataAware {
    private Map<String, ByteBuf> metadataStore = new HashMap<>();
    /**
     * routing metadata, cached to avoid parse again
     */
    private GSVRoutingMetadata routingMetadata;
    /**
     * data encoding metadata, cached to avoid parse again
     */
    private MessageMimeTypeMetadata encodingMetadata;
    /**
     * accept mimetypes metadata
     */
    private MessageAcceptMimeTypesMetadata acceptMimeTypesMetadata;

    public static RSocketCompositeMetadata from(ByteBuf content) {
        RSocketCompositeMetadata temp = new RSocketCompositeMetadata();
        if (content.capacity() > 0) {
            temp.load(content);
        }
        return temp;
    }

    public static RSocketCompositeMetadata from(MetadataAware... metadataList) {
        RSocketCompositeMetadata temp = new RSocketCompositeMetadata();
        for (MetadataAware metadataAware : metadataList) {
            temp.addMetadata(metadataAware);
        }
        return temp;
    }

    public RSocketCompositeMetadata() {
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.CompositeMetadata;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.CompositeMetadata.getType();
    }

    @Override
    public ByteBuf getContent() {
        CompositeByteBuf compositeByteBuf = PooledByteBufAllocator.DEFAULT.compositeBuffer();
        for (Map.Entry<String, ByteBuf> entry : metadataStore.entrySet()) {
            WellKnownMimeType wellKnownMimeType = WellKnownMimeType.fromString(entry.getKey());
            if (wellKnownMimeType != UNPARSEABLE_MIME_TYPE) {
                CompositeMetadataFlyweight.encodeAndAddMetadata(compositeByteBuf, ByteBufAllocator.DEFAULT, wellKnownMimeType, entry.getValue());
            } else {
                CompositeMetadataFlyweight.encodeAndAddMetadata(compositeByteBuf, ByteBufAllocator.DEFAULT, entry.getKey(), entry.getValue());
            }
        }
        return compositeByteBuf;
    }


    @Override
    public void load(ByteBuf byteBuf) {
        CompositeMetadata compositeMetadata = new CompositeMetadata(byteBuf, false);
        for (CompositeMetadata.Entry entry : compositeMetadata) {
            metadataStore.put(entry.getMimeType(), entry.getContent());
        }
    }

    public ByteBuf getMetadata(RSocketMimeType mimeType) {
        return metadataStore.get(mimeType.getType());
    }

    public boolean contains(RSocketMimeType mimeType) {
        return metadataStore.containsKey(mimeType.getType());
    }

    public RSocketCompositeMetadata addMetadata(MetadataAware metadataSupport) {
        metadataStore.put(metadataSupport.getMimeType(), metadataSupport.getContent());
        return this;
    }

    @Nullable
    public GSVRoutingMetadata getRoutingMetaData() {
        if (this.routingMetadata == null && metadataStore.containsKey(RSocketMimeType.Routing.getType())) {
            this.routingMetadata = new GSVRoutingMetadata();
            ByteBuf byteBuf = metadataStore.get(RSocketMimeType.Routing.getType());
            routingMetadata.load(byteBuf);
        }
        return routingMetadata;
    }

    @Nullable
    public MessageMimeTypeMetadata getDataEncodingMetadata() {
        if (this.encodingMetadata == null && metadataStore.containsKey(RSocketMimeType.MessageMimeType.getType())) {
            this.encodingMetadata = new MessageMimeTypeMetadata();
            ByteBuf byteBuf = metadataStore.get(RSocketMimeType.MessageMimeType.getType());
            this.encodingMetadata.load(byteBuf);
        }
        return encodingMetadata;
    }

    @Nullable
    public MessageAcceptMimeTypesMetadata getAcceptMimeTypesMetadata() {
        if (this.acceptMimeTypesMetadata == null && metadataStore.containsKey(RSocketMimeType.MessageAcceptMimeTypes.getType())) {
            this.acceptMimeTypesMetadata = new MessageAcceptMimeTypesMetadata();
            ByteBuf byteBuf = metadataStore.get(RSocketMimeType.MessageAcceptMimeTypes.getType());
            this.acceptMimeTypesMetadata.load(byteBuf);
        }
        return acceptMimeTypesMetadata;
    }


    @Override
    public String toText() throws Exception {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, ByteBuf> entry : metadataStore.entrySet()) {
            RSocketMimeType metadataType = RSocketMimeType.valueOfType(entry.getKey());
            ByteBuf byteBuf = entry.getValue();
            switch (metadataType) {
                case Application: {
                    AppMetadata metadata = new AppMetadata();
                    metadata.load(byteBuf);
                    lines.add(metadataType.getName() + ":" + metadata.toText());
                    break;
                }
                case MessageMimeType: {
                    MessageMimeTypeMetadata metadata = new MessageMimeTypeMetadata();
                    metadata.load(byteBuf);
                    lines.add(metadataType.getName() + ":" + metadata.toText());
                    break;
                }
                case BearerToken: {
                    BearerTokenMetadata metadata = new BearerTokenMetadata();
                    metadata.load(byteBuf);
                    lines.add(metadataType.getName() + ":" + metadata.toText());
                    break;
                }
                case Routing: {
                    GSVRoutingMetadata metadata = new GSVRoutingMetadata();
                    metadata.load(byteBuf);
                    lines.add(metadataType.getName() + ":" + metadata.toText());
                    break;
                }
                case Tracing: {
                    TracingMetadata metadata = new TracingMetadata();
                    metadata.load(byteBuf);
                    lines.add(metadataType.getName() + ":" + metadata.toText());
                    break;
                }
                case CacheControl: {
                    CacheControlMetadata metadata = new CacheControlMetadata();
                    metadata.load(byteBuf);
                    lines.add(metadataType.getName() + ":" + metadata.toText());
                    break;
                }
            }
        }
        return String.join("\r", lines);
    }

    @Override
    public void load(String text) throws Exception {
        String[] lines = text.split("\r");
        for (String line : lines) {
            String[] parts = line.split("\\s*:\\s*", 2);
            if (parts.length > 1) {
                RSocketMimeType metadataType = RSocketMimeType.valueOfType(parts[0].trim());
                switch (metadataType) {
                    case Application: {
                        AppMetadata metadata = new AppMetadata();
                        metadata.load(parts[1].trim());
                        addMetadata(metadata);
                        break;
                    }
                    case MessageMimeType: {
                        MessageMimeTypeMetadata metadata = new MessageMimeTypeMetadata();
                        metadata.load(parts[1].trim());
                        addMetadata(metadata);
                        break;
                    }
                    case BearerToken: {
                        BearerTokenMetadata metadata = new BearerTokenMetadata();
                        metadata.load(parts[1].trim());
                        addMetadata(metadata);
                        break;
                    }
                    case Routing: {
                        GSVRoutingMetadata metadata = new GSVRoutingMetadata();
                        metadata.load(parts[1].trim());
                        addMetadata(metadata);
                        break;
                    }
                    case Tracing: {
                        TracingMetadata metadata = new TracingMetadata();
                        metadata.load(parts[1].trim());
                        addMetadata(metadata);
                        break;
                    }
                    case CacheControl: {
                        CacheControlMetadata metadata = new CacheControlMetadata();
                        metadata.load(parts[1].trim());
                        addMetadata(metadata);
                        break;
                    }
                }
            }
        }
    }

}
