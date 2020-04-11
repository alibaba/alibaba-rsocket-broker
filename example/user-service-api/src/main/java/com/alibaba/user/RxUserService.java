package com.alibaba.user;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.Date;
import java.util.List;

/**
 * user reactive service with RxJava2 API
 *
 * @author leijuan
 */
public interface RxUserService {

    /**
     * RPC call to get user
     *
     * @param id user
     * @return user
     */
    Maybe<User> findById(Integer id);

    /**
     * find by email or phone
     *
     * @param email email or phone
     * @param phone phone
     * @return user
     */
    Maybe<User> findByEmailOrPhone(String email, String phone);

    /**
     * find all to test list
     *
     * @return user list
     */
    Single<List<User>> findAll();

    /**
     * save or update user
     *
     * @param user user
     * @return user's id
     */
    Single<Integer> save(User user);

    /**
     * RPC call without parameters
     *
     * @return result
     */
    Single<String> getAppName();

    /**
     * rpc call, you want to deal success result: result.doOnSuccess(s -> { }).subscribe();
     *
     * @return Mono void
     */
    Maybe<Void> job1();

    /**
     * fire & forget operation
     *
     * @param name name
     */
    void flush(String name);

    /**
     * request/stream to get people by type
     *
     * @param type type
     * @return user stream
     */
    Flowable<User> findAllPeople(String type);

    /**
     * channel(bi-direction stream)
     *
     * @param point point
     * @return user
     */
    Observable<User> recent(Observable<Date> point);

}
