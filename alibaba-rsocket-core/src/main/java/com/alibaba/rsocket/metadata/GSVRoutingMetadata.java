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
    public static String SERVICE_METHOD_SEPARATOR = "-";
    public static int SERVICE_METHOD_SEPARATOR_LENGTH = SERVICE_METHOD_SEPARATOR.length();
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
        this.group = group;
        this.service = service;
        this.method = method;
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
        tags.add(service + SERVICE_METHOD_SEPARATOR + method);
        if (group != null && !group.isEmpty()) {
            tags.add("g:" + group);
        }
        if (version != null && !version.isEmpty()) {
            tags.add("v:" + version);
        }
        if (endpoint != null && !endpoint.isEmpty()) {
            tags.add("e:" + endpoint);
        }
        return TaggingMetadataFlyweight.createTaggingContent(ByteBufAllocator.DEFAULT, tags);
    }

    /**
     * format routing as "group:service:version:endpoint", separated by colon style
     *
     * @return data format
     */
    public String formatData() {
        StringBuilder builder = new StringBuilder();
        String serviceFullName = service + SERVICE_METHOD_SEPARATOR + method;
        builder.append(serviceFullName);
        builder.append("\n").append("g:").append(group);
        builder.append("\n").append("v:").append("version");
        builder.append(version);
        if (endpoint != null) {
            builder.append("\n").append("e:").append(endpoint);
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
            String routing = iterator.next();
            if (routing.contains(SERVICE_METHOD_SEPARATOR)) {
                int poundIndex = routing.lastIndexOf(SERVICE_METHOD_SEPARATOR);
                this.service = routing.substring(0, poundIndex);
                this.method = routing.substring(poundIndex + SERVICE_METHOD_SEPARATOR_LENGTH);
            } else {
                this.service = routing;
            }
        }
        while (iterator.hasNext()) {
            String tag = iterator.next();
            if (tag.startsWith("g:")) {
                this.group = tag.substring(2);
            } else if (tag.startsWith("v:")) {
                this.version = tag.substring(2);
            } else if (tag.startsWith("e:")) {
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
        String[] parts = text.split(":", 4);
        group = parts[0];
        String fullServiceName = parts[1];
        if (fullServiceName.contains(SERVICE_METHOD_SEPARATOR)) {
            int poundIndex = fullServiceName.lastIndexOf(SERVICE_METHOD_SEPARATOR);
            this.service = fullServiceName.substring(0, poundIndex);
            this.method = fullServiceName.substring(poundIndex + SERVICE_METHOD_SEPARATOR_LENGTH);
        } else {
            this.service = fullServiceName;
        }
        version = parts[2];
        if (parts.length > 3) {
            endpoint = parts[3];
        }
    }

    public static GSVRoutingMetadata from(ByteBuf content) {
        GSVRoutingMetadata temp = new GSVRoutingMetadata();
        temp.load(content);
        return temp;
    }
}
