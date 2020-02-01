package com.alibaba.rsocket.encoding;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;

/**
 * hessian utils
 *
 * @author leijuan
 */
public class HessianUtils {

    public static ByteBuf outputAsBuffer(Object obj) throws Exception {
        if (obj == null) {
            return Unpooled.EMPTY_BUFFER;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HessianSerializerOutput output = new HessianSerializerOutput(bos);
        output.writeObject(obj);
        output.flush();
        return Unpooled.wrappedBuffer(bos.toByteArray());
    }

    public static Object decode(ByteBuf buffer) throws Exception {
        HessianSerializerInput input = new HessianSerializerInput(new ByteBufInputStream(buffer));
        return input.readObject();
    }

}
