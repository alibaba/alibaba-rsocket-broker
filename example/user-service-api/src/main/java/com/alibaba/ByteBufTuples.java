package com.alibaba;

import io.netty.buffer.ByteBuf;
import reactor.util.function.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * ByteBuf Tuples
 *
 * @author leijuan
 */
@SuppressWarnings("unchecked")
public class ByteBufTuples {
    public static <T1, T2> Tuple2<T1, T2> of(ByteBuf buf, Class<T1> t1, Class<T2> t2) {
        return (Tuple2<T1, T2>) Tuples.of(read(buf, t1), read(buf, t2));
    }

    public static <T1, T2, T3> Tuple3<T1, T2, T3> of(ByteBuf buf, T1 t1, T2 t2, T3 t3) {
        return (Tuple3<T1, T2, T3>) Tuples.of(read(buf, t1), read(buf, t2), read(buf, t3));
    }

    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> of(ByteBuf buf, T1 t1, T2 t2, T3 t3, T4 t4) {
        return (Tuple4<T1, T2, T3, T4>) Tuples.of(read(buf, t1), read(buf, t2), read(buf, t3), read(buf, t4));
    }

    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> of(
            ByteBuf buf,
            T1 t1,
            T2 t2,
            T3 t3,
            T4 t4,
            T5 t5) {
        return (Tuple5<T1, T2, T3, T4, T5>) Tuples.of(read(buf, t1), read(buf, t2), read(buf, t3), read(buf, t4), read(buf, t4));
    }


    public static <T> Object read(ByteBuf buf, T type) {
        if (type.equals(Byte.class)) {
            return buf.readByte();
        } else if (type.equals(Integer.class)) {
            return buf.readInt();
        } else if (type.equals(Long.class)) {
            return buf.readLong();
        } else if (type.equals(Double.class)) {
            return buf.readDouble();
        } else if (type.equals(Boolean.class)) {
            return buf.readInt() == 1;
        } else if (type.equals(String.class)) {
            int len = buf.readInt();
            if (len == 0) {
                return "";
            } else {
                return buf.readCharSequence(len, StandardCharsets.UTF_8).toString();
            }
        } else if (type == Date.class) {
            return new Date(buf.readLong());
        }
        throw new RuntimeException(type.getClass().getCanonicalName() + " illegal");
    }
}
