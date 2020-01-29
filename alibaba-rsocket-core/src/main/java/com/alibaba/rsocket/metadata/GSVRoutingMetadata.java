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
    private String group = "";
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
    private String version = "";
    /**
     * endpoint
     */
    private String endpoint;

    public GSVRoutingMetadata() {
    }

    public GSVRoutingMetadata(String group, String service, String method, String version) {
        this.group = group == null ? "" : group;
        this.setService(service);
        this.method = method;
        this.version = version == null ? "" : version;
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
        if (service.contains("-")) {
            this.service = service.substring(0, service.indexOf("-"));
            this.method = service.substring(service.indexOf("-"));
        } else {
            this.service = service;
        }
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
        tags.add((group == null ? "" : group) + ":" + service + ":" + (version == null ? "" : version));
        if (method != null && !method.isEmpty()) {
            tags.add("m=" + method);
        }
        if (endpoint != null && !endpoint.isEmpty()) {
            tags.add("e=" + endpoint);
        }
        return TaggingMetadataFlyweight.createTaggingContent(ByteBufAllocator.DEFAULT, tags);
    }

    /**
     * format routing as "group:service:version?m=login&e=xxxx"
     *
     * @return data format
     */
    public String formatData() {
        StringBuilder builder = new StringBuilder();
        String serviceFullName = group + ":" + service + ":" + version;
        builder.append(serviceFullName);
        if (method != null) {
            builder.append("?").append("m=").append(method);
        }
        if (endpoint != null) {
            builder.append("&").append("e=").append(endpoint);
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
        String routing;
        String tags = null;
        if (text.contains("?")) {
            routing = text.substring(0, text.indexOf("?"));
            tags = text.substring(text.indexOf("?") + 1);
        } else {
            routing = text;
        }
        //routing
        parseRoutingKey(routing);
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

    private void parseRoutingKey(String routing) {
        if (routing.contains(":")) {
            String[] parts = routing.split(":");
            this.group = parts[0];
            this.service = parts[1];
            if (parts.length > 2) {
                this.version = parts[2];
            }
        } else {
            this.service = routing;
        }
    }

    public static GSVRoutingMetadata from(ByteBuf content) {
        GSVRoutingMetadata temp = new GSVRoutingMetadata();
        temp.load(content);
        return temp;
    }
}
