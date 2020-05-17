package com.alibaba.rsocket.encoding.impl;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.Serializable;
import kotlinx.serialization.SerializationStrategy;
import kotlinx.serialization.cbor.Cbor;
import kotlinx.serialization.protobuf.ProtoBuf;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Kotlin Serializer support
 *
 * @author leijuan
 */
@SuppressWarnings("unchecked")
public class KotlinSerializerSupport {
    private static final Map<Class<?>, KSerializer<?>> ktSerializableClassStore = new HashMap<>();

    protected static KSerializer<?> getSerializer(Class<?> clazz) throws Exception {
        KSerializer<?> kSerializer = ktSerializableClassStore.get(clazz);
        if (kSerializer == null) {
            Class<?> serializerClazz = Class.forName(clazz.getCanonicalName() + "$$serializer");
            Field instanceField = serializerClazz.getDeclaredField("INSTANCE");
            kSerializer = (KSerializer<?>) instanceField.get(null);
            ktSerializableClassStore.put(clazz, kSerializer);
        }
        return kSerializer;
    }

    protected static boolean isKotlinSerializable(Class<?> clazz) {
        return ktSerializableClassStore.containsKey(clazz) || clazz.getAnnotation(Serializable.class) != null;
    }

    public static byte[] encodeAsProtobuf(Object result) throws Exception {
        return ProtoBuf.Default.dump((SerializationStrategy<? super Object>) KotlinSerializerSupport.getSerializer(result.getClass()), result);
    }

    public static Object decodeFromProtobuf(byte[] bytes, Class<?> targetClass) throws Exception {
        return ProtoBuf.Default.load(KotlinSerializerSupport.getSerializer(targetClass), bytes);
    }

    public static byte[] encodeAsCbor(Object result) throws Exception {
        return Cbor.Default.dump((SerializationStrategy<? super Object>) KotlinSerializerSupport.getSerializer(result.getClass()), result);
    }

    public static Object decodeFromCbor(byte[] bytes, Class<?> targetClass) throws Exception {
        return Cbor.Default.load(KotlinSerializerSupport.getSerializer(targetClass), bytes);
    }

}
