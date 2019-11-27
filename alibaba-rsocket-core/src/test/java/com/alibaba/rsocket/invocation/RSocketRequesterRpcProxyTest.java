package com.alibaba.rsocket.invocation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;


/**
 * RSocket Requester RPC Proxy test
 *
 * @author leijuan
 */
public class RSocketRequesterRpcProxyTest {

    @Test
    public void testCacheKey() {
        //array hash code
        Object[] args1 = new Object[]{"demo", "1", 12};
        Object[] args2 = new Object[]{"demo", "1", 12};
        System.out.println("args1 hashcode:" + Arrays.hashCode(args1));
        System.out.println("args1 Deep hashcode:" + Arrays.deepHashCode(args1));
        System.out.println("args2 hashcode:" + Arrays.hashCode(args2));
        System.out.println("args2 Deep hashcode:" + Arrays.deepHashCode(args2));
        //array with null values
        System.out.println("Null Deep hashcode:" + Arrays.deepHashCode(null));
        System.out.println("Empty Array Deep hashcode:" + Arrays.deepHashCode(new Object[]{}));
        System.out.println("Array2 with all Null Deep hashcode:" + Arrays.deepHashCode(new Object[]{null, null}));
        System.out.println("Array2 with all Null Deep hashcode:" + Arrays.deepHashCode(new Object[]{null, null}));
        System.out.println("Array4 with all Null Deep hashcode:" + Arrays.deepHashCode(new Object[]{null, null, null, null}));
        //primitive code
        System.out.println("Hashcode for 'good morning': " + "good morning".hashCode());
        System.out.println("Hashcode for 'good morning': " + "good morning".hashCode());
    }
}
