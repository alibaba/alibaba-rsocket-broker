package com.alibaba.spring.boot.rsocket.demo.controllers;

import com.alibaba.user.UserService;
import com.alibaba.user.UserService2;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/bench")
public class BenchmarkController {
    private static Logger log = LoggerFactory.getLogger(BenchmarkController.class);
    @Autowired
    private UserService userService;
    @Autowired
    private UserService2 userService2;

    @GetMapping("/service1/{count}")
    public Mono<Void> callByBroker(@PathVariable("count") int count) {
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
                .doFinally(signalType -> {
                    histogram.outputPercentileDistribution(System.out, 1000.0d);
                    double completedMillis = (System.nanoTime() - start) / 1_000_000d;
                    double tps = count / (completedMillis / 1_000d);
                    log.info("Request by broker complete in {} ms", completedMillis);
                    log.info("Request by broker tps {}", tps);
                }).then();
    }

    @GetMapping("/service2/{count}")
    public Mono<Void> callByDirect(@PathVariable("count") int count) {
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
                .doFinally(signalType -> {
                    histogram.outputPercentileDistribution(System.out, 1000.0d);
                    double completedMillis = (System.nanoTime() - start) / 1_000_000d;
                    double tps = count / (completedMillis / 1_000d);
                    log.info("Request by direct complete in {} ms", completedMillis);
                    log.info("Request by direct tps {}", tps);
                }).then();
    }
}
