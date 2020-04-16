package com.alibaba.rsocket.encoding.impl;

import kotlinx.serialization.KSerializer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Kotlin Serializer support
 *
 * @author leijuan
 */
public class KotlinSerializerSupport {
    protected Map<Class<?>, KSerializer<?>> ktSerializableClassStore = new HashMap<>();

    protected KSerializer<?> getSerializer(Class<?> clazz) throws Exception {
        KSerializer<?> kSerializer = ktSerializableClassStore.get(clazz);
        if (kSerializer == null) {
            Class<?> serializerClazz = Class.forName(clazz.getCanonicalName() + "$$serializer");
            Field instanceField = serializerClazz.getDeclaredField("INSTANCE");
            kSerializer = (KSerializer<?>) instanceField.get(null);
            ktSerializableClassStore.put(clazz, kSerializer);
        }
        return kSerializer;
    }
}
