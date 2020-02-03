package com.alibaba.rsocket.metadata;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.utils.MurmurHash3;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.metadata.TaggingMetadataFlyweight;

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
        if (routeKey.contains(".")) {
            int offset = routeKey.lastIndexOf('.');
            this.service = routeKey.substring(0, offset);
            this.method = routeKey.substring(offset + 1);
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

    public Integer id() {
        return MurmurHash3.hash32(ServiceLocator.serviceId(group, service, group));
    }

    public String routing() {
        return ServiceLocator.serviceId(group, service, group);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
        if (method != null && !method.isEmpty()) {
            tags.add("m=" + method);
        }
        if (endpoint != null && !endpoint.isEmpty()) {
            tags.add("e=" + endpoint);
        }
        return TaggingMetadataFlyweight.createTaggingContent(ByteBufAllocator.DEFAULT, tags);
    }

    /**
     * format routing as "group!service:version?m=login&e=xxxx"
     *
     * @return data format
     */
    public String formatData() {
        StringBuilder builder = new StringBuilder();
        builder.append(assembleRoutingKey());
        if (method != null || endpoint != null) {
            builder.append("?");
            if (!method.isEmpty()) {
                builder.append("m=").append(method);
                builder.append("&");
            }
            if (!endpoint.isEmpty()) {
                builder.append("e=").append(endpoint);
            }
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return formatData();
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
            String tag = iterator.next();
            if (tag.startsWith("m=")) {
                this.method = tag.substring(2);
            } else if (tag.startsWith("e=")) {
                this.endpoint = tag.substring(2);
            }
        }
    }

    @Override
    public String toText() throws Exception {
        return formatData();
    }

    @Override
    public void load(String text) {
        String routingKey;
        String tags = null;
        if (text.contains("?")) {
            routingKey = text.substring(0, text.indexOf("?"));
            tags = text.substring(text.indexOf("?") + 1);
        } else {
            routingKey = text;
        }
        //routingKey
        parseRoutingKey(routingKey);
        if (tags != null) {
            for (String tag : tags.split("&")) {
                if (tag.startsWith("m=")) {
                    this.method = tag.substring(2);
                } else if (tag.startsWith("e=")) {
                    this.endpoint = tag.substring(2);
                }
            }
        }
    }

    private void parseRoutingKey(String routingKey) {
        String temp = routingKey;
        //group
        if (temp.contains("!")) {
            this.group = temp.substring(0, temp.indexOf('!'));
            temp = temp.substring(temp.indexOf('!') + 1);
        }
        //version
        if (temp.contains(":")) {
            this.version = temp.substring(temp.lastIndexOf(':') + 1);
            temp = temp.substring(0, temp.indexOf(':'));
        }
        //service & method
        if (temp.contains(".")) {
            int offset = temp.lastIndexOf('.');
            this.service = temp.substring(0, offset);
            this.method = temp.substring(offset + 1);
        } else {
            this.service = temp;
        }
    }

    private String assembleRoutingKey() {
        StringBuilder routingBuilder = new StringBuilder();
        //group
        if (group != null && !group.isEmpty()) {
            routingBuilder.append(group).append("!");
        }
        //service
        routingBuilder.append(service);
        //method
        if (method != null && !method.isEmpty()) {
            routingBuilder.append(".").append(method);
        }
        //version
        if (version != null && !version.isEmpty()) {
            routingBuilder.append(":").append(version);
        }
        return routingBuilder.toString();
    }

    public static GSVRoutingMetadata from(ByteBuf content) {
        GSVRoutingMetadata temp = new GSVRoutingMetadata();
        temp.load(content);
        return temp;
    }
}
