package com.alibaba.rsocket.invocation;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.rsocket.ServiceMapping;
import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.reactive.ReactiveAdapter;
import com.alibaba.rsocket.reactive.ReactiveMethodSupport;
import com.alibaba.rsocket.utils.MurmurHash3;
import io.micrometer.core.instrument.Tag;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.frame.FrameType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * reactive method metadata for service interface
 *
 * @author leijuan
 */
public class ReactiveMethodMetadata extends ReactiveMethodSupport {
    public static final List<String> STREAM_CLASSES = Arrays.asList("io.reactivex.Flowable", "io.reactivex.Observable",
            "io.reactivex.rxjava3.core.Observable", "io.reactivex.rxjava3.core.Flowable", "reactor.core.publisher.Flux");
    /**
     * service full name, format as com.alibaba.user.UserService
     */
    private String service;
    /**
     * method handler name
     */
    private String name;
    /**
     * full name, service and name
     */
    private String fullName;
    /**
     * service group
     */
    private String group;
    /**
     * service version
     */
    private String version;
    /**
     * service ID
     */
    private Integer serviceId;
    /**
     * method handler id
     */
    private Integer handlerId;
    /**
     * endpoint
     */
    private String endpoint;
    /**
     * rsocket frame type
     */
    private FrameType rsocketFrameType;
    /**
     * parameters encoding type
     */
    private RSocketMimeType paramEncoding;
    /**
     * accept encoding
     */
    private RSocketMimeType[] acceptEncodingTypes;
    /**
     * default composite metadata for RSocket, and include routing, encoding & accept encoding
     */
    private RSocketCompositeMetadata compositeMetadata;
    /**
     * bytebuf for default composite metadata
     */
    private ByteBuf compositeMetadataByteBuf;
    /**
     * metrics tags
     */
    private List<Tag> metricsTags = new ArrayList<>();
    /**
     * reactive adapter for RxJava2 & RxJava3 etc
     */
    private ReactiveAdapter reactiveAdapter;
    /**
     * channel with Mono return type
     */
    private boolean monoChannel = false;

    public ReactiveMethodMetadata(String group, String service, String version,
                                  Method method,
                                  @NotNull RSocketMimeType dataEncodingType,
                                  @NotNull RSocketMimeType[] acceptEncodingTypes,
                                  @Nullable String endpoint) {
        super(method);
        this.service = service;
        this.name = method.getName();
        this.fullName = this.service + "." + this.name;
        this.group = group;
        this.version = version;
        this.endpoint = endpoint;
        //deal with @ServiceMapping for method
        ServiceMapping serviceMapping = method.getAnnotation(ServiceMapping.class);
        if (serviceMapping != null) {
            initServiceMapping(serviceMapping);
        }
        this.serviceId = MurmurHash3.hash32(ServiceLocator.serviceId(group, service, version));
        this.handlerId = MurmurHash3.hash32(service + "." + name);
        //param encoding type
        this.paramEncoding = dataEncodingType;
        this.acceptEncodingTypes = acceptEncodingTypes;
        //byte buffer binary encoding
        if (paramCount == 1) {
            Class<?> parameterType = method.getParameterTypes()[0];
            if (BINARY_CLASS_LIST.contains(parameterType)) {
                this.paramEncoding = RSocketMimeType.Binary;
            }
        }
        //init composite metadata for invocation
        initCompositeMetadata();
        //bi direction check: param's type is Flux for 1st param or 2nd param
        if (paramCount == 1 && method.getParameterTypes()[0].equals(Flux.class)) {
            rsocketFrameType = FrameType.REQUEST_CHANNEL;
        } else if (paramCount == 2 && method.getParameterTypes()[1].equals(Flux.class)) {
            rsocketFrameType = FrameType.REQUEST_CHANNEL;
        }
        if (rsocketFrameType != null && rsocketFrameType == FrameType.REQUEST_CHANNEL) {
            if (method.getReturnType().isAssignableFrom(Mono.class)) {
                this.monoChannel = true;
            }
        }
        if (this.rsocketFrameType == null) {
            assert inferredClassForReturn != null;
            // fire_and_forget
            if (returnType.equals(Void.TYPE) || (returnType.equals(Mono.class) && inferredClassForReturn.equals(Void.TYPE))) {
                this.rsocketFrameType = FrameType.REQUEST_FNF;
            } else if (returnType.equals(Flux.class) || STREAM_CLASSES.contains(returnType.getCanonicalName())) {  // request/stream
                this.rsocketFrameType = FrameType.REQUEST_STREAM;
            } else { //request/response
                this.rsocketFrameType = FrameType.REQUEST_RESPONSE;
            }
        }
        //reactive adapter for return type
        this.reactiveAdapter = ReactiveAdapter.findAdapter(returnType.getCanonicalName());
        //metrics tags for micrometer
        if (this.group != null && !this.group.isEmpty()) {
            metricsTags.add(Tag.of("group", this.group));
        }
        if (this.version != null && !this.version.isEmpty()) {
            metricsTags.add(Tag.of("version", this.version));
        }
        metricsTags.add(Tag.of("method", this.name));
        metricsTags.add(Tag.of("frame", String.valueOf(this.rsocketFrameType.getEncodedType())));
    }

    public void initServiceMapping(ServiceMapping serviceMapping) {
        if (!serviceMapping.value().isEmpty()) {
            String serviceName = serviceMapping.value();
            if (serviceName.contains(".")) {
                this.service = serviceName.substring(0, serviceName.lastIndexOf('.'));
                this.name = serviceName.substring(serviceName.lastIndexOf('.') + 1);
            } else {
                this.name = serviceName;
            }
        }
        if (!serviceMapping.group().isEmpty()) {
            this.group = serviceMapping.group();
        }
        if (!serviceMapping.version().isEmpty()) {
            this.version = serviceMapping.version();
        }
        if (!serviceMapping.endpoint().isEmpty()) {
            this.endpoint = serviceMapping.endpoint();
        }
        if (!serviceMapping.paramEncoding().isEmpty()) {
            this.paramEncoding = RSocketMimeType.valueOfType(serviceMapping.paramEncoding());
        }
        if (!serviceMapping.resultEncoding().isEmpty()) {
            this.acceptEncodingTypes = new RSocketMimeType[]{RSocketMimeType.valueOfType(serviceMapping.resultEncoding())};
        }
    }

    public void initCompositeMetadata() {
        //payload routing metadata
        GSVRoutingMetadata routingMetadata = new GSVRoutingMetadata(group, this.service, this.name, version);
        routingMetadata.setEndpoint(this.endpoint);
        //payload binary routing metadata
        BinaryRoutingMetadata binaryRoutingMetadata = new BinaryRoutingMetadata(this.serviceId, this.handlerId,
                routingMetadata.assembleRoutingKey().getBytes(StandardCharsets.UTF_8));
        //add param encoding
        MessageMimeTypeMetadata messageMimeTypeMetadata = new MessageMimeTypeMetadata(this.paramEncoding);
        //set accepted mimetype
        MessageAcceptMimeTypesMetadata messageAcceptMimeTypesMetadata = new MessageAcceptMimeTypesMetadata(this.acceptEncodingTypes);
        //construct default composite metadata
        CompositeByteBuf compositeMetadataContent;
        this.compositeMetadata = RSocketCompositeMetadata.from(routingMetadata, messageMimeTypeMetadata, messageAcceptMimeTypesMetadata);
        //add gsv routing data if endpoint not empty
        if (endpoint != null && !endpoint.isEmpty()) {
            this.compositeMetadata.addMetadata(binaryRoutingMetadata);
            compositeMetadataContent = (CompositeByteBuf) this.compositeMetadata.getContent();
        } else {
            compositeMetadataContent = (CompositeByteBuf) this.compositeMetadata.getContent();
            //add BinaryRoutingMetadata as first
            compositeMetadataContent.addComponent(true, 0, binaryRoutingMetadata.getHeaderAndContent());
        }
        // convert composite bytebuf to bytebuf for performance
        this.compositeMetadataByteBuf = Unpooled.copiedBuffer(compositeMetadataContent);
        ReferenceCountUtil.safeRelease(compositeMetadataContent);
    }

    public String getFullName() {
        return fullName;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public ReactiveAdapter getReactiveAdapter() {
        return reactiveAdapter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FrameType getRsocketFrameType() {
        return rsocketFrameType;
    }

    public boolean isMonoChannel() {
        return monoChannel;
    }

    public Class<?> getInferredClassForReturn() {
        return inferredClassForReturn;
    }


    public RSocketMimeType getParamEncoding() {
        return paramEncoding;
    }

    public void setParamEncoding(RSocketMimeType paramEncoding) {
        this.paramEncoding = paramEncoding;
    }

    public RSocketMimeType[] getAcceptEncodingTypes() {
        return acceptEncodingTypes;
    }

    public void setAcceptEncodingTypes(RSocketMimeType[] acceptEncodingTypes) {
        this.acceptEncodingTypes = acceptEncodingTypes;
    }

    public RSocketCompositeMetadata getCompositeMetadata() {
        return this.compositeMetadata;
    }

    /**
     * get default composite metadata ByteBuf for remote call. please use .retainedDuplicate() if necessary
     *
     * @return composite metadata ByteBuf
     */
    public ByteBuf getCompositeMetadataByteBuf() {
        return compositeMetadataByteBuf;
    }

    public List<Tag> getMetricsTags() {
        return this.metricsTags;
    }

}
