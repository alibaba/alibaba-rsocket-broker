package com.alibaba.rsocket.encoding;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;

/**
 * Hessian en/decode utils
 *
 * @author leijuan
 */
public class HessianUtils {

    @NotNull
    public static ByteBuf encode(@Nullable Object obj) throws Exception {
        if (obj == null) {
            return Unpooled.EMPTY_BUFFER;
        }
        //bos and output close not necessary
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HessianSerializerOutput output = new HessianSerializerOutput(bos);
        output.writeObject(obj);
        output.flush();
        return Unpooled.wrappedBuffer(bos.toByteArray());
    }

    @Nullable
    public static Object decode(@Nullable ByteBuf byteBuf) throws Exception {
        if (byteBuf == null || byteBuf.capacity() == 0) {
            return null;
        }
        return new HessianSerializerInput(new ByteBufInputStream(byteBuf)).readObject();
    }

}
