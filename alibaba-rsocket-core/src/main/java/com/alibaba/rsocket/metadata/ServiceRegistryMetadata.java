package com.alibaba.rsocket.metadata;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.encoding.JsonUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.HashSet;
import java.util.Set;

/**
 * service registry metadata: subscribed and published services from requester
 *
 * @author leijuan
 */
public class ServiceRegistryMetadata implements MetadataAware {
    /**
     * published services
     */
    private Set<ServiceLocator> published = new HashSet<>();
    /**
     * subscribed services
     */
    private Set<ServiceLocator> subscribed = new HashSet<>();

    public void addPublishedService(ServiceLocator publishedService) {
        this.published.add(publishedService);
    }

    public void addSubscribedService(ServiceLocator subscribedService) {
        this.subscribed.add(subscribedService);
    }

    public Set<ServiceLocator> getPublished() {
        return published;
    }

    public void setPublished(Set<ServiceLocator> published) {
        this.published = published;
    }

    public Set<ServiceLocator> getSubscribed() {
        return subscribed;
    }

    public void setSubscribed(Set<ServiceLocator> subscribed) {
        this.subscribed = subscribed;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.ServiceRegistry;
    }

    @Override
    @JsonIgnore
    public String getMimeType() {
        return RSocketMimeType.ServiceRegistry.getType();
    }

    @Override
    @JsonIgnore
    public ByteBuf getContent() {
        try {
            return JsonUtils.toJsonByteBuf(this);
        } catch (Exception e) {
            return Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public void load(ByteBuf byteBuf) throws Exception {
        JsonUtils.updateJsonValue(byteBuf, this);
    }
    
    public boolean containPublishedServices() {
        return published != null && !published.isEmpty();
    }

    public static ServiceRegistryMetadata from(ByteBuf content) {
        ServiceRegistryMetadata temp = new ServiceRegistryMetadata();
        try {
            temp.load(content);
        } catch (Exception ignore) {

        }
        return temp;
    }
}
