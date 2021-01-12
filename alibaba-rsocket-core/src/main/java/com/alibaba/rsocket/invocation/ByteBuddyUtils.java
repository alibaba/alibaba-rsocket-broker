package com.alibaba.rsocket.invocation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * ByteBuddy Utils
 *
 * @author leijuan
 */
public class ByteBuddyUtils {
    @SuppressWarnings("unchecked")
    public static <T> T build(Class<T> serviceInterface, Object proxy) {
        Class<T> dynamicType = (Class<T>) new ByteBuddy(ClassFileVersion.JAVA_V8)
                .subclass(serviceInterface)
                .name(serviceInterface.getSimpleName() + "RSocketStub")
                .method(ElementMatchers.not(ElementMatchers.isDefaultMethod()))
                .intercept(MethodDelegation.to(proxy))
                .make()
                .load(serviceInterface.getClassLoader())
                .getLoaded();
        try {
            return dynamicType.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
