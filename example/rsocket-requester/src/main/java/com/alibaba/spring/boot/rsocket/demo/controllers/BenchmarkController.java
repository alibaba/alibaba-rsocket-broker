package com.alibaba.spring.boot.rsocket.demo.controllers;

import com.alibaba.user.UserService;
import com.alibaba.user.UserService2;
import org.HdrHistogram.Histogram;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/bench")
public class BenchmarkController {
    @Autowired
    private UserService userService;
    @Autowired
    private UserService2 userService2;

    @GetMapping("/serviceByBroker/{count}")
    public Mono<String> callByBroker(@PathVariable("count") int count) {
        Histogram histogram = new Histogram(3600000000000L, 3);
        long start = System.nanoTime();
        return Flux.range(1, count)
                .flatMap(number -> {
                    long s = System.nanoTime();
                    return userService.findById(number).doFinally(signalType -> {
                        histogram.recordValue(System.nanoTime() - s);
                    });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.create(sink -> {
                    double completedMillis = (System.nanoTime() - start) / 1_000_000d;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    PrintStream output = new PrintStream(bos);
                    histogram.outputPercentileDistribution(output, 1000.0d);
                    double tps = count / (completedMillis / 1_000d);
                    try {
                        output.write(String.format("Request by broker complete %s in %s ms\n", count, completedMillis).getBytes(StandardCharsets.UTF_8));
                        output.write(String.format("Request by broker tps %s\n", tps).getBytes(StandardCharsets.UTF_8));
                    } catch (Exception ignore) {

                    }
                    sink.success(bos.toString());
                }));
    }

    @GetMapping("/serviceByDirect/{count}")
    public Mono<String> callByDirect(@PathVariable("count") int count) {
        Histogram histogram = new Histogram(3600000000000L, 3);
        long start = System.nanoTime();
        return Flux.range(1, count)
                .flatMap(number -> {
                    long s = System.nanoTime();
                    return userService2.findById(number).doFinally(signalType -> {
                        histogram.recordValue(System.nanoTime() - s);
                    });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.create(sink -> {
                    double completedMillis = (System.nanoTime() - start) / 1_000_000d;
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    PrintStream output = new PrintStream(bos);
                    histogram.outputPercentileDistribution(output, 1000.0d);
                    double tps = count / (completedMillis / 1_000d);
                    try {
                        output.write(String.format("Request by direct complete %s in %s ms\n", count, completedMillis).getBytes(StandardCharsets.UTF_8));
                        output.write(String.format("Request by direct tps %s\n", tps).getBytes(StandardCharsets.UTF_8));
                    } catch (Exception ignore) {

                    }
                    sink.success(bos.toString());
                }));
    }
}
