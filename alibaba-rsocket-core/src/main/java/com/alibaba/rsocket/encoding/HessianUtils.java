package com.alibaba.rsocket.encoding;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * hessian utils
 *
 * @author leijuan
 */
public class HessianUtils {

    public static byte[] output(Object obj) {
        if (obj == null) {
            return new byte[]{};
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HessianSerializerOutput output = new HessianSerializerOutput(bos);
            output.writeObject(obj);
            output.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ByteBuf outputAsBuffer(Object obj) {
        if (obj == null) {
            return Unpooled.EMPTY_BUFFER;
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HessianSerializerOutput output = new HessianSerializerOutput(bos);
            output.writeObject(obj);
            output.flush();
            return Unpooled.wrappedBuffer(bos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object decode(byte[] content) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(content);
            HessianSerializerInput input = new HessianSerializerInput(bis);
            return input.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object decode(ByteBuf buffer) throws Exception {
        HessianSerializerInput input = new HessianSerializerInput(new ByteBufInputStream(buffer));
        return input.readObject();
    }

}
