package com.alibaba.rsocket.loadbalance;

import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * random selector
 *
 * @author leijuan
 */
public class RandomSelector<T> implements Supplier<Mono<T>> {
    // atomic性能更好一些
    //private Random randomGenerator = new Random();
    private AtomicInteger position = new AtomicInteger(0);
    private List<T> elements;
    private int size;
    private String name;

    public RandomSelector(String name, List<T> elements) {
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

    @Override
    public Mono<T> get() {
        T next = next();
        if (next == null) {
            return Mono.error(new NoAvailableConnectionException(RsocketErrorCode.message("RST-200404", this.name)));
        }
        return Mono.just(next);
    }
}
