package com.alibaba.rsocket.invocation;

import com.alibaba.rsocket.metadata.*;
import com.alibaba.rsocket.reactive.ReactiveAdapter;
import io.micrometer.core.instrument.Tag;
import io.rsocket.frame.FrameType;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * reactive method metadata for service interface
 *
 * @author leijuan
 */
public class ReactiveMethodMetadata {
    public static List<String> STREAM_CLASSES = Arrays.asList("io.reactivex.Flowable", "io.reactivex.Observable",
            "io.reactivex.rxjava3.core.Observable", "io.reactivex.rxjava3.core.Flowable", "reactor.core.publisher.Flux");
    /**
     * service full name
     */
    private String serviceFullName;
    /**
     * method handler
     */
    private String name;
    /**
     * service group
     */
    private String group;
    /**
     * service version
     */
    private String version;
    private int parameterCount;
    /**
     * rsocket frame type
     */
    private FrameType rsocketFrameType;
    /**
     * param encoding
     */
    private RSocketMimeType paramEncoding;
    /**
     * default composite metadata for method: routing, encoding & accept encoding
     */
    private RSocketCompositeMetadata defaultCompositeMetadata;
    /**
     * metrics tag
     */
    private List<Tag> metricsTags = new ArrayList<>();
    /**
     * return type
     */
    private Class<?> returnType;
    /**
     * inferred class for return type
     */
    private Class<?> inferredClassForReturn;
    private ReactiveAdapter reactiveAdapter;

    public ReactiveMethodMetadata(Class<?> interfaceClass, Method method, RSocketMimeType defaultEncoding, String group, String version, @Nullable RSocketMimeType encodingType) {
        this.serviceFullName = interfaceClass.getCanonicalName();
        this.name = method.getName();
        this.group = group;
        this.version = version;
        //result type & generic type
        this.returnType = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType != null) {
            //http://tutorials.jenkov.com/java-reflection/generics.html
            if (genericReturnType instanceof ParameterizedType) {
                ParameterizedType type = (ParameterizedType) genericReturnType;
                Type[] typeArguments = type.getActualTypeArguments();
                if (typeArguments.length > 0) {
                    final Type typeArgument = typeArguments[0];
                    if (typeArgument instanceof ParameterizedType) {
                        this.inferredClassForReturn = (Class<?>) ((ParameterizedType) typeArgument).getActualTypeArguments()[0];
                    } else {
                        this.inferredClassForReturn = (Class<?>) typeArgument;
                    }
                }
            }
            if (this.inferredClassForReturn == null) {
                this.inferredClassForReturn = (Class<?>) genericReturnType;
            }
        }
        this.parameterCount = method.getParameterCount();
        //encoding type
        if (encodingType != null) {
            this.paramEncoding = encodingType;
        } else {
            if (parameterCount == 1) {
                this.paramEncoding = defaultEncoding;
            }
            if (paramEncoding == null) {
                this.paramEncoding = defaultEncoding;
            }
        }
        //payload routing metadata
        GSVRoutingMetadata routing = new GSVRoutingMetadata(group, this.serviceFullName, this.name, version);
        //add param encoding
        MessageMimeTypeMetadata messageMimeTypeMetadata = new MessageMimeTypeMetadata(this.paramEncoding);
        //set accepted mimetype
        MessageAcceptMimeTypesMetadata messageAcceptMimeTypesMetadata = new MessageAcceptMimeTypesMetadata(this.paramEncoding);
        //construct default composite metadata
        this.defaultCompositeMetadata = RSocketCompositeMetadata.from(routing, messageMimeTypeMetadata, messageAcceptMimeTypesMetadata);
        //bi direction check: type is Flux for 1st param or type is flux for 2nd param
        if (parameterCount == 1 && method.getParameterTypes()[0].equals(Flux.class)) {
            rsocketFrameType = FrameType.REQUEST_CHANNEL;
        } else if (parameterCount == 2 && method.getParameterTypes()[1].equals(Flux.class)) {
            rsocketFrameType = FrameType.REQUEST_CHANNEL;
        }
        if (this.rsocketFrameType == null) {
            assert inferredClassForReturn != null;
            if (returnType.equals(Void.TYPE) || (returnType.equals(Mono.class) && inferredClassForReturn.equals(Void.TYPE))) {
                this.rsocketFrameType = FrameType.REQUEST_FNF;
            } else if (returnType.equals(Flux.class) || STREAM_CLASSES.contains(returnType.getCanonicalName())) {
                this.rsocketFrameType = FrameType.REQUEST_STREAM;
            } else {
                this.rsocketFrameType = FrameType.REQUEST_RESPONSE;
            }
        }
        this.reactiveAdapter = ReactiveAdapter.findAdapter(returnType.getCanonicalName());
        //metrics tags
        if (this.group != null && !this.group.isEmpty()) {
            metricsTags.add(Tag.of("group", this.group));
        }
        if (this.version != null && !this.version.isEmpty()) {
            metricsTags.add(Tag.of("version", this.version));
        }
        metricsTags.add(Tag.of("method", this.name));
        metricsTags.add(Tag.of("frame", String.valueOf(this.rsocketFrameType.getEncodedType())));
    }

    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
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

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public FrameType getRsocketFrameType() {
        return rsocketFrameType;
    }

    public Class<?> getInferredClassForReturn() {
        return inferredClassForReturn;
    }

    public void setInferredClassForReturn(Class<?> inferredClassForReturn) {
        this.inferredClassForReturn = inferredClassForReturn;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public void setParameterCount(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    public RSocketMimeType getParamEncoding() {
        return paramEncoding;
    }

    public void setParamEncoding(RSocketMimeType paramEncoding) {
        this.paramEncoding = paramEncoding;
    }

    public RSocketCompositeMetadata getDefaultCompositeMetadata() {
        return this.defaultCompositeMetadata;
    }

    public List<Tag> getMetricsTags() {
        return this.metricsTags;
    }

}
