package com.alibaba.rsocket.listen;


import org.jetbrains.annotations.Nullable;

/**
 * attribute map
 *
 * @author leijuan
 */
public interface AttributeMap {

    @Nullable
    Object attr(String name);

    void attr(String name, @Nullable Object value);
}
