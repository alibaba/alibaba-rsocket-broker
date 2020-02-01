package com.alibaba.user;


import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;

/**
 * user reactive service with RxJava3 API
 *
 * @author leijuan
 */
public interface Rx3UserService {

    /**
     * RPC call to get user
     *
     * @param id user
     * @return user
     */
    Maybe<User> findById(Integer id);

    /**
     * request/stream to get people by type
     *
     * @param type type
     * @return user stream
     */
    Flowable<User> findAllPeople(String type);

}
