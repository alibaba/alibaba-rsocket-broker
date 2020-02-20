package com.alibaba.rsocket.encoding;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import io.netty.buffer.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Hessian en/decode utils
 *
 * @author leijuan
 */
public class HessianUtils {

    @NotNull
    public static ByteBuf encode(@Nullable Object obj) throws IOException {
        if (obj == null) {
            return Unpooled.EMPTY_BUFFER;
        }
        //bos and output close not necessary
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
        ByteBufOutputStream bos = new ByteBufOutputStream(byteBuf);
        bos.flush();
        HessianSerializerOutput output = new HessianSerializerOutput(bos);
        output.writeObject(obj);
        output.flush();
        return byteBuf;
    }

    @Nullable
    public static Object decode(@Nullable ByteBuf byteBuf) throws Exception {
        if (byteBuf == null || byteBuf.capacity() == 0) {
            return null;
        }
        return new HessianSerializerInput(new ByteBufInputStream(byteBuf)).readObject();
    }

}
