package com.alibaba.spring.boot.rsocket.broker.supporting;

import com.alibaba.rsocket.rpc.LocalReactiveServiceCallerImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * RSocketLocalService annotation processor
 *
 * @author leijuan
 */
public class RSocketLocalServiceAnnotationProcessor extends LocalReactiveServiceCallerImpl implements BeanPostProcessor {


    public RSocketLocalServiceAnnotationProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        scanRSocketServiceAnnotation(bean, beanName);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    protected void scanRSocketServiceAnnotation(Object bean, String beanName) {
        Class<?> managedBeanClass = bean.getClass();
        RSocketLocalService reactiveService = AnnotationUtils.findAnnotation(managedBeanClass, RSocketLocalService.class);
        if (reactiveService != null) {
            registerRSocketService(reactiveService, bean);
        }
    }

    private void registerRSocketService(RSocketLocalService rSocketService, Object bean) {
        String serviceName = rSocketService.name();
        if (serviceName.isEmpty()) {
            serviceName = rSocketService.serviceInterface().getCanonicalName();
        }
        addProvider("", serviceName, "", rSocketService.serviceInterface(), bean);
    }


}
