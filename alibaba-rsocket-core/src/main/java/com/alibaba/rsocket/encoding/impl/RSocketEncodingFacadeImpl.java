package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.encoding.EncodingException;
import com.alibaba.rsocket.encoding.ObjectEncodingHandler;
import com.alibaba.rsocket.encoding.RSocketEncodingFacade;
import com.alibaba.rsocket.metadata.MessageMimeTypeMetadata;
import com.alibaba.rsocket.metadata.RSocketCompositeMetadata;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.observability.RsocketErrorCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import static com.alibaba.rsocket.encoding.ObjectEncodingHandler.EMPTY_BUFFER;

/**
 * RSocket encoding facade implementation
 *
 * @author leijuan
 */
public class RSocketEncodingFacadeImpl implements RSocketEncodingFacade {
    private Logger log = LoggerFactory.getLogger(RSocketEncodingFacadeImpl.class);
    private Map<RSocketMimeType, ObjectEncodingHandler> handlerMap = new HashMap<>();
    /**
     * composite metadata ByteBuf for message mime types
     */
    private Map<RSocketMimeType, ByteBuf> compositeMetadataForMimeTypes = new HashMap<>();

    public static final RSocketEncodingFacade instance = new RSocketEncodingFacadeImpl();

    public RSocketEncodingFacadeImpl() {
        ServiceLoader<ObjectEncodingHandler> serviceLoader = ServiceLoader.load(ObjectEncodingHandler.class);
        for (ObjectEncodingHandler objectEncodingHandler : serviceLoader) {
            RSocketMimeType mimeType = objectEncodingHandler.mimeType();
            handlerMap.put(mimeType, objectEncodingHandler);
            RSocketCompositeMetadata resultCompositeMetadata = RSocketCompositeMetadata.from(new MessageMimeTypeMetadata(mimeType));
            ByteBuf compositeMetadataContent = resultCompositeMetadata.getContent();
            this.compositeMetadataForMimeTypes.put(mimeType, Unpooled.copiedBuffer(compositeMetadataContent));
            ReferenceCountUtil.safeRelease(compositeMetadataContent);
        }
    }

    @NotNull
    @Override
    public ByteBuf encodingParams(@Nullable Object[] args, RSocketMimeType encodingType) {
        try {
            ObjectEncodingHandler handler = handlerMap.get(encodingType);
            return handler.encodingParams(args);
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-700500", "Object[]", encodingType.getName()), e);
            return EMPTY_BUFFER;
        }
    }

    @Override
    public @Nullable Object decodeParams(RSocketMimeType encodingType, @Nullable ByteBuf data, @Nullable Class<?>... targetClasses) {
        try {
            if (data == null || data.readableBytes() == 0) return null;
            return handlerMap.get(encodingType).decodeParams(data, targetClasses);
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-700501", encodingType.getName(), "Object[]"), e);
            return null;
        }
    }

    @NotNull
    @Override
    public ByteBuf encodingResult(@Nullable Object result, RSocketMimeType encodingType) throws EncodingException {
        try {
            return handlerMap.get(encodingType).encodingResult(result);
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-700500", result != null ? result.getClass() : "Null", encodingType.getName()), e);
            return EMPTY_BUFFER;
        }
    }

    @Override
    public @Nullable Object decodeResult(RSocketMimeType encodingType, @Nullable ByteBuf data, @Nullable Class<?> targetClass) {
        try {
            if (data == null || data.readableBytes() == 0) return null;
            //convert to raw output without decoding
            if (targetClass == ByteBuffer.class) {
                return data.nioBuffer();
            } else if (targetClass == ByteBuf.class) {
                return data;
            }
            return handlerMap.get(encodingType).decodeResult(data, targetClass);
        } catch (Exception e) {
            log.error(RsocketErrorCode.message("RST-700501", encodingType.getName(), targetClass != null ? targetClass.getName() : "Null"), e);
            return null;
        }
    }

    @Override
    public ByteBuf getDefaultCompositeMetadataByteBuf(RSocketMimeType messageMimeType) {
        return this.compositeMetadataForMimeTypes.get(messageMimeType);
    }

    //check encoding type exist or not
    private void checkMimeTypeAvailable(RSocketMimeType encodingType) throws EncodingException {
        if (!handlerMap.containsKey(encodingType)) {
            String message = RsocketErrorCode.message("RST-700405", encodingType.getType());
            throw new EncodingException(message, new Exception(message));
        }
    }
}
