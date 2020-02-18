package com.alibaba.rsocket.loadbalance;

import com.alibaba.rsocket.observability.RsocketErrorCode;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Supplier;

/**
 * random selector
 *
 * @author leijuan
 */
public class RandomSelector<T> implements Supplier<Mono<T>> {
    // round robin index
    private int index;
    private List<T> elements;
    private int size;
    private String name;

    public RandomSelector(String name, List<T> elements) {
        this.elements = elements;
        this.size = elements.size();
        this.name = name;
        this.index = 0;
    }

    @Nullable
    public T next() {
        if (size == 0) {
            return null;
        } else if (size == 1) {
            return elements.get(0);
        } else {
            T t = elements.get(index);
            index = (index + 1) % size;
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
