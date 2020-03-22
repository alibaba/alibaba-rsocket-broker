package com.alibaba.rsocket.gateway;

import com.alibaba.account.Account;
import com.alibaba.account.ReactorAccountServiceGrpc;
import com.google.protobuf.Int32Value;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;


/**
 * ByteBuddy Demo test
 *
 * @author leijuan
 */
@SuppressWarnings("unchecked")
public class ByteBuddyProxyTest {
    @Test
    public void testGrpcProxy() throws Exception {
        DemoInterceptor interceptor = new DemoInterceptor();
        Class<ReactorAccountServiceGrpc.AccountServiceImplBase> dynamicType = (Class<ReactorAccountServiceGrpc.AccountServiceImplBase>) new ByteBuddy()
                .subclass(ReactorAccountServiceGrpc.AccountServiceImplBase.class)
                .method(ElementMatchers.returns(target -> target.isAssignableFrom(Mono.class) || target.isAssignableFrom(Flux.class)))
                .intercept(MethodDelegation.to(interceptor))
                .make()
                .load(getClass().getClassLoader())
                .getLoaded();
        System.out.println(dynamicType.newInstance().findById(Mono.just(Int32Value.newBuilder().setValue(1).build())).block());
    }

    public static class DemoInterceptor {
        @RuntimeType
        public Object intercept(@Origin Method method, @AllArguments Object[] params) {
            String methodName = method.getName();
            if (methodName.equals("findById")) {
                return Mono.just(Account.newBuilder().setId(1).setNick("demo").build());
            } else if (methodName.equals("findByStatus") || methodName.equals("findByIdStream")) {
                return Flux.just(Account.newBuilder().setId(1).setNick("demo").build());
            }
            return null;
        }
    }
}

