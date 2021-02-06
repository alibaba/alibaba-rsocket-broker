package com.alibaba.spring.boot.rsocket.responder.invocation;

import com.alibaba.rsocket.RSocketService;
import com.alibaba.rsocket.rpc.LocalReactiveServiceCallerImpl;
import com.alibaba.rsocket.spring.SpringRSocketService;
import com.alibaba.spring.boot.rsocket.RSocketProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * RSocketService annotation processor
 *
 * @author leijuan
 */
public class RSocketServiceAnnotationProcessor extends LocalReactiveServiceCallerImpl implements BeanPostProcessor {
    private RSocketProperties rsocketProperties;

    public RSocketServiceAnnotationProcessor(RSocketProperties rsocketProperties) {
        this.rsocketProperties = rsocketProperties;
    }

    @Override
    public Object postProcessBeforeInitialization(@NotNull Object bean, String beanName) throws BeansException {
        scanRSocketServiceAnnotation(bean, beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(@NotNull Object bean, String beanName) throws BeansException {
        return bean;
    }

    protected void scanRSocketServiceAnnotation(Object bean, String beanName) {
        Class<?> managedBeanClass = bean.getClass();
        RSocketService reactiveService = AnnotationUtils.findAnnotation(managedBeanClass, RSocketService.class);
        if (reactiveService != null) {
            registerRSocketService(reactiveService, bean);
        }
        SpringRSocketService springRSocketService = AnnotationUtils.findAnnotation(managedBeanClass, SpringRSocketService.class);
        if (springRSocketService != null) {
            registerRSocketService(springRSocketService, bean);
        }
    }

    private void registerRSocketService(RSocketService rsocketServiceAnnotation, Object bean) {
        String serviceName = rsocketServiceAnnotation.serviceInterface().getCanonicalName();
        if (!rsocketServiceAnnotation.name().isEmpty()) {
            serviceName = rsocketServiceAnnotation.name();
        }
        String group = rsocketServiceAnnotation.group().isEmpty() ? rsocketProperties.getGroup() : rsocketServiceAnnotation.group();
        String version = rsocketServiceAnnotation.version().isEmpty() ? rsocketProperties.getVersion() : rsocketServiceAnnotation.version();
        addProvider(group, serviceName, version, rsocketServiceAnnotation.serviceInterface(), bean);
    }

    private void registerRSocketService(SpringRSocketService rsocketServiceAnnotation, Object bean) {
        Class<?> serviceInterface = rsocketServiceAnnotation.serviceInterface();
        if (serviceInterface != Void.class) {
            String serviceName = rsocketServiceAnnotation.serviceInterface().getCanonicalName();
            if (rsocketServiceAnnotation.value().length > 0) {
                serviceName = rsocketServiceAnnotation.value()[0];
            }
            String group = rsocketServiceAnnotation.group().isEmpty() ? rsocketProperties.getGroup() : rsocketServiceAnnotation.group();
            String version = rsocketServiceAnnotation.version().isEmpty() ? rsocketProperties.getVersion() : rsocketServiceAnnotation.version();
            addProvider(group, serviceName, version, serviceInterface, bean);
        }
    }

}
