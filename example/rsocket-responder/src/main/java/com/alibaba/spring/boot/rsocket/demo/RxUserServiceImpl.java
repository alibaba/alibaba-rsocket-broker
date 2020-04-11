package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.RSocketService;
import com.alibaba.user.RxUserService;
import com.alibaba.user.User;
import com.github.javafaker.Faker;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Rx User service implementation
 *
 * @author leijuan
 */
@RSocketService(serviceInterface = RxUserService.class)
@Service
public class RxUserServiceImpl implements RxUserService {
    private Faker faker = new Faker();

    @Override
    public Maybe<User> findById(Integer id) {
        return Maybe.fromCallable(() -> {
            User user = new User();
            user.setId(id);
            user.setNick(faker.name().name());
            user.setPhone(faker.phoneNumber().cellPhone());
            user.setEmail(faker.internet().emailAddress());
            return user;
        });
    }

    @Override
    public Maybe<User> findByEmailOrPhone(String email, String phone) {
        return Maybe.fromCallable(() -> {
            User user = new User();
            user.setId(1);
            user.setEmail(email);
            user.setPhone(phone);
            user.setNick("nick1");
            return user;
        });
    }

    @Override
    public Single<List<User>> findAll() {
        return Single.just(Arrays.asList(new User(1, "first"), new User(2, "second")));
    }

    @Override
    public Single<Integer> save(User user) {
        return Single.just(1);
    }

    @Override
    public Single<String> getAppName() {
        return Single.just("Demo1");
    }

    @Override
    public Maybe<Void> job1() {
        return Maybe.empty();
    }

    @Override
    public void flush(String name) {

    }

    @Override
    public Flowable<User> findAllPeople(String type) {
        return Flowable.interval(1, TimeUnit.SECONDS)
                .map(timestamp -> new User((int) (timestamp % 1000), "nick:" + type));
    }

    @Override
    public Observable<User> recent(Observable<Date> point) {
        return Observable.just(new User(1, "first"), new User(2, "second"));
    }
}
