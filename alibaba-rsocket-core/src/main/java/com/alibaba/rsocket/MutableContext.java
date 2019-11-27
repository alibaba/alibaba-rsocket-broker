package com.alibaba.rsocket;

import org.jetbrains.annotations.NotNull;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Mutable context
 *
 * @author leijuan
 */
public class MutableContext implements Context {
    HashMap<Object, Object> holder = new HashMap<>();

    @SuppressWarnings("unchecked")
    @NotNull
    @Override
    public <T> T get(@NotNull Object key) {
        return (T) holder.get(key);
    }

    @Override
    public boolean hasKey(@NotNull Object key) {
        return holder.containsKey(key);
    }

    @NotNull
    @Override
    public Context put(@NotNull Object key, @NotNull Object value) {
        holder.put(key, value);
        return this;
    }

    @NotNull
    @Override
    public Context delete(@NotNull Object key) {
        holder.remove(key);
        return this;
    }

    @Override
    public int size() {
        return holder.size();
    }

    @NotNull
    @Override
    public Stream<Map.Entry<Object, Object>> stream() {
        return holder.entrySet().stream();
    }

}
