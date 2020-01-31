package com.alibaba.rsocket.loadbalance;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 * Random Selector test
 *
 * @author leijuan
 */
public class RandomSelectorTest {
    private static RandomSelector<Integer> randomSelector = new RandomSelector<>("Demo", Arrays.asList(1, 2, 3));

    @Test
    public void testNext() {
        for (int i = 0; i < 100; i++) {
            System.out.println(randomSelector.next());
        }
    }
}
