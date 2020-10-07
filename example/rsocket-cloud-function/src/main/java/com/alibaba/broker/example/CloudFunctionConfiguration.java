package com.alibaba.broker.example;

import com.alibaba.rsocket.RSocketRequesterSupport;
import com.alibaba.spring.boot.rsocket.EnvironmentProperties;
import com.alibaba.spring.boot.rsocket.RSocketProperties;
import com.alibaba.spring.boot.rsocket.upstream.RSocketRequesterSupportBuilderImpl;
import com.alibaba.spring.boot.rsocket.upstream.RSocketRequesterSupportCustomizer;
import io.rsocket.SocketAcceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Cloud Function configuration
 *
 * @author leijuan
 */
@Configuration
public class CloudFunctionConfiguration {

    @Bean("com.alibaba.WordService.uppercase")
    public Function<String, String> uppercase() {
        return String::toUpperCase;
    }

    @Bean("com.alibaba.WordService.lowercase")
    public Function<String, Mono<String>> lowercase() {
        return input -> Mono.just(input.toLowerCase());
    }

    @Bean
    public RSocketRequesterSupport rsocketRequesterSupport(@Autowired RSocketProperties properties,
                                                           @Autowired Environment environment,
                                                           @Autowired SocketAcceptor socketAcceptor,
                                                           @Autowired ObjectProvider<RSocketRequesterSupportCustomizer> customizers,
                                                           @Autowired FunctionRegistry functionRegistry) {
        EnvironmentProperties env = new EnvironmentProperties(environment);
        RSocketRequesterSupportBuilderImpl builder = new RSocketRequesterSupportBuilderImpl(properties, env, socketAcceptor);
        builder.requesterSupport(new RSocketRequesterFunctionSupport(properties, env, socketAcceptor, functionRegistry));
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    public LocalReactiveFunctionCaller localReactiveFunctionCaller(@Autowired FunctionRegistry functionRegistry) {
        return new LocalReactiveFunctionCaller(functionRegistry);
    }

}
