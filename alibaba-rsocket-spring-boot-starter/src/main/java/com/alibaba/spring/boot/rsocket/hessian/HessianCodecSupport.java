package com.alibaba.spring.boot.rsocket.hessian;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Collections;
import java.util.List;

/**
 * Hessian Codec support
 *
 * @author leijuan
 */
public class HessianCodecSupport {
    public static final MimeType HESSIAN_MIME_TYPE = MimeTypeUtils.parseMimeType("application/x-hessian");
    public static final List<MimeType> HESSIAN_MIME_TYPES = Collections.singletonList(HESSIAN_MIME_TYPE);


    public Object decode(DataBuffer dataBuffer) throws Exception {
        return new HessianSerializerInput(dataBuffer.asInputStream()).readObject();
    }

    public DataBuffer encode(Object obj, DataBufferFactory bufferFactory) throws Exception {
        DataBuffer dataBuffer = bufferFactory.allocateBuffer();
        HessianSerializerOutput output = new HessianSerializerOutput(dataBuffer.asOutputStream());
        output.writeObject(obj);
        output.flush();
        return dataBuffer;
    }
}
