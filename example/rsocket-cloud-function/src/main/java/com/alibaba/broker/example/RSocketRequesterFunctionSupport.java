package com.alibaba.broker.example;

import com.alibaba.rsocket.ServiceLocator;
import com.alibaba.spring.boot.rsocket.RSocketProperties;
import com.alibaba.spring.boot.rsocket.RSocketRequesterSupportImpl;
import io.rsocket.SocketAcceptor;
import org.springframework.cloud.function.context.FunctionRegistry;

import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * RSocket requester support for Spring Cloud Function
 *
 * @author leijuan
 */
public class RSocketRequesterFunctionSupport extends RSocketRequesterSupportImpl {
    private FunctionRegistry functionRegistry;

    public RSocketRequesterFunctionSupport(RSocketProperties properties, Properties env,
                                           SocketAcceptor socketAcceptor, FunctionRegistry functionRegistry) {
        super(properties, env, socketAcceptor);
        this.functionRegistry = functionRegistry;
    }

    @Override
    public Supplier<Set<ServiceLocator>> exposedServices() {
        return () -> functionRegistry.getNames(null)
                .stream().filter(functionName -> functionName.contains("."))
                .map(functionName -> functionName.substring(0, functionName.lastIndexOf(".")))
                .collect(Collectors.toSet())
                .stream()
                .map(serviceName -> {
                            String members = functionRegistry.getNames(null).stream().filter(f -> f.startsWith(serviceName + "."))
                                    .map(f -> f.substring(f.lastIndexOf(".") + 1))
                                    .collect(Collectors.joining(","));
                            return new ServiceLocator(properties.getGroup(), serviceName, properties.getVersion(),
                                    new String[]{"type: function", "members:" + members});
                        }
                )
                .collect(Collectors.toSet());
    }
}
