package com.alibaba.rsocket.loadbalance;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * random selector
 *
 * @author leijuan
 */
public class RandomSelector<T> {
    // atomic性能更好一些
    //private Random randomGenerator = new Random();
    private AtomicInteger position = new AtomicInteger(0);
    private List<T> elements;
    private int size;

    public RandomSelector(List<T> elements) {
        this.elements = elements;
        this.size = elements.size();
    }

    @Nullable
    public T next() {
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return elements.get(0);
        } else {
            T t = elements.get((this.position.incrementAndGet() & 0x7FFFFFFF) % size);
            if (t == null) {
                t = elements.get(0);
            }
            return t;
        }
    }
}
