package com.alibaba.spring.boot.rsocket;

import com.alibaba.rsocket.listen.RSocketListener;
import com.alibaba.rsocket.listen.RSocketListenerCustomizer;
import io.rsocket.SocketAcceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RSocket listener configuration
 *
 * @author leijuan
 */
@Configuration
@ConditionalOnExpression("${rsocket.port:0}!=0 && ${rsocket.disabled:false}==false")
public class RSocketListenerAutoConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RSocketListener rsocketListener(ObjectProvider<RSocketListenerCustomizer> customizers) {
        RSocketListener.Builder builder = RSocketListener.builder();
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    public RSocketListenerCustomizer defaultRSocketListenerCustomizer(@Autowired SocketAcceptor socketAcceptor, @Autowired RSocketProperties properties) {
        return builder -> {
            builder.acceptor(socketAcceptor);
            builder.listen(properties.getSchema(), properties.getPort());
        };
    }

}
