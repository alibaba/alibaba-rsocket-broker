package com.alibaba.rsocket.rpc

import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import java.lang.reflect.Method
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction

/**
 * Kotlin suspend call to Mono
 * @param obj object
 * @param method Java Method
 * @param args arguments
 * @return Mono
 */
fun suspendCallToMono(obj: Any, method: Method, vararg args: Any?): Mono<Any> {
    return mono {
        method.kotlinFunction!!.callSuspend(obj, *args)
    }
}