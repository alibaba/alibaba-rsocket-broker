package com.alibaba.rsocket.metadata;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.utils.MurmurHash3;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.metadata.TaggingMetadataCodec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GSV routing metadata, format as tagging routing data
 *
 * @author leijuan
 */
public class GSVRoutingMetadata implements MetadataAware {
    /**
     * group: region, datacenter, virtual group in datacenter
     */
    private String group;
    /**
     * service name
     */
    private String service;
    /**
     * method name
     */
    private String method;
    /**
     * version
     */
    private String version;
    /**
     * endpoint
     */
    private String endpoint;
    /**
     * sticky session
     */
    private boolean sticky;
    /**
     * target instance ID
     */
    private transient Integer targetId;

    public GSVRoutingMetadata() {
    }

    public GSVRoutingMetadata(String group, String service, String method, String version) {
        this.group = group;
        this.service = service;
        this.method = method;
        this.version = version;
    }

    public GSVRoutingMetadata(String group, String routeKey, String version) {
        this.group = group;
        int methodSymbolPosition = routeKey.lastIndexOf('.');
        if (methodSymbolPosition > 0) {
            this.service = routeKey.substring(0, methodSymbolPosition);
            this.method = routeKey.substring(methodSymbolPosition + 1);
        } else {
            this.service = routeKey;
        }
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public Integer id() {
        if (targetId != null) return targetId;
        if (group == null && version == null) {
            return MurmurHash3.hash32(service);
        } else {
            return MurmurHash3.hash32(ServiceLocator.serviceId(group, service, version));
        }
    }

    public Integer handlerId() {
        return MurmurHash3.hash32(service + "." + method);
    }

    public String gsv() {
        return ServiceLocator.serviceId(group, service, version);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    @Override
    public RSocketMimeType rsocketMimeType() {
        return RSocketMimeType.Routing;
    }

    @Override
    public String getMimeType() {
        return RSocketMimeType.Routing.getType();
    }

    @Override
    public ByteBuf getContent() {
        List<String> tags = new ArrayList<>();
        tags.add(assembleRoutingKey());
        //method included in routing key
        /*if (method != null && !method.isEmpty()) {
            tags.add("m=" + method);
        }*/
        if (endpoint != null && !endpoint.isEmpty()) {
            tags.add("e=" + endpoint);
        }
        if (sticky) {
            tags.add("sticky=1");
        }
        return TaggingMetadataCodec.createTaggingContent(PooledByteBufAllocator.DEFAULT, tags);
    }

    /**
     * parse data
     *
     * @param byteBuf byte buffer
     */
    public void load(ByteBuf byteBuf) {
        Iterator<String> iterator = new RoutingMetadata(byteBuf).iterator();
        //first tag is routing for service name or method
        if (iterator.hasNext()) {
            parseRoutingKey(iterator.next());
        }
        while (iterator.hasNext()) {
            parseTag(iterator.next());
        }
    }

    private void parseRoutingKey(String routingKey) {
        String temp = routingKey;
        String tags = null;
        if (routingKey.contains("?")) {
            temp = routingKey.substring(0, routingKey.indexOf("?"));
            tags = routingKey.substring(routingKey.indexOf("?") + 1);
        }
        //group parse
        int groupSymbolPosition = temp.indexOf('!');
        if (groupSymbolPosition > 0) {
            this.group = temp.substring(0, groupSymbolPosition);
            temp = temp.substring(groupSymbolPosition + 1);
        }
        //version
        int versionSymbolPosition = temp.lastIndexOf(':');
        if (versionSymbolPosition > 0) {
            this.version = temp.substring(versionSymbolPosition + 1);
            temp = temp.substring(0, versionSymbolPosition);
        }
        //service & method
        int methodSymbolPosition = temp.lastIndexOf('.');
        if (methodSymbolPosition > 0) {
            this.service = temp.substring(0, methodSymbolPosition);
            this.method = temp.substring(methodSymbolPosition + 1);
        } else {
            this.service = temp;
        }
        if (tags != null) {
            String[] tagParts = tags.split("&");
            for (String tagPart : tagParts) {
                parseTag(tagPart);
            }
        }
    }

    private void parseTag(String tag) {
        if (tag.startsWith("m=")) {
            this.method = tag.substring(2);
        } else if (tag.startsWith("e=")) {
            this.endpoint = tag.substring(2);
        } else if (tag.equalsIgnoreCase("sticky=1")) {
            this.sticky = true;
        }
    }

    public String assembleRoutingKey() {
        StringBuilder routingBuilder = new StringBuilder();
        //group
        if (group != null && !group.isEmpty()) {
            routingBuilder.append(group).append('!');
        }
        //service
        routingBuilder.append(service);
        //method
        if (method != null && !method.isEmpty()) {
            routingBuilder.append('.').append(method);
        }
        //version
        if (version != null && !version.isEmpty()) {
            routingBuilder.append(':').append(version);
        }
        if (this.sticky || this.endpoint != null) {
            routingBuilder.append("?");
            if (this.sticky) {
                routingBuilder.append("sticky=1&");
            }
            if (this.endpoint != null) {
                routingBuilder.append("e=").append(endpoint).append("&");
            }
        }
        return routingBuilder.toString();
    }

    public static GSVRoutingMetadata from(ByteBuf content) {
        GSVRoutingMetadata temp = new GSVRoutingMetadata();
        temp.load(content);
        return temp;
    }

    public static GSVRoutingMetadata from(String routingKey) {
        GSVRoutingMetadata temp = new GSVRoutingMetadata();
        temp.parseRoutingKey(routingKey);
        return temp;
    }
}
