package com.alibaba.rsocket.encoding.impl;

import kotlinx.serialization.KSerializer;
import kotlinx.serialization.Serializable;
import kotlinx.serialization.SerializationStrategy;
import kotlinx.serialization.cbor.Cbor;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.protobuf.ProtoBuf;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Kotlin Serializer support
 *
 * @author leijuan
 */
@SuppressWarnings("unchecked")
public class KotlinSerializerSupport {
    private static final Map<Class<?>, KSerializer<?>> ktSerializableClassStore = new HashMap<>();
    private static final Set<Class<?>> normalClassSet = new HashSet<>();

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
        if (ktSerializableClassStore.containsKey(clazz)) {
            return true;
        } else if (normalClassSet.contains(clazz)) {
            return false;
        } else {
            if (clazz.getAnnotation(Serializable.class) != null) {
                try {
                    getSerializer(clazz);
                } catch (Exception ignore) {
                }
                return true;
            } else {
                normalClassSet.add(clazz);
                return false;
            }
        }
    }

    public static byte[] encodeAsProtobuf(Object result) throws Exception {
        return ProtoBuf.Default.encodeToByteArray((SerializationStrategy<? super Object>) KotlinSerializerSupport.getSerializer(result.getClass()), result);
    }

    public static Object decodeFromProtobuf(byte[] bytes, Class<?> targetClass) throws Exception {
        return ProtoBuf.Default.decodeFromByteArray(KotlinSerializerSupport.getSerializer(targetClass), bytes);
    }

    public static byte[] encodeAsCbor(Object result) throws Exception {
        return Cbor.Default.encodeToByteArray((SerializationStrategy<? super Object>) KotlinSerializerSupport.getSerializer(result.getClass()), result);
    }

    public static Object decodeFromCbor(byte[] bytes, Class<?> targetClass) throws Exception {
        return Cbor.Default.decodeFromByteArray(KotlinSerializerSupport.getSerializer(targetClass), bytes);
    }

    public static byte[] encodeAsJson(Object result) throws Exception {
        return Json.Default.encodeToString((SerializationStrategy<? super Object>) KotlinSerializerSupport.getSerializer(result.getClass()), result)
                .getBytes(StandardCharsets.UTF_8);
    }

    public static Object decodeFromJson(byte[] bytes, Class<?> targetClass) throws Exception {
        return Json.Default.decodeFromString(KotlinSerializerSupport.getSerializer(targetClass), new String(bytes, StandardCharsets.UTF_8));
    }

}
