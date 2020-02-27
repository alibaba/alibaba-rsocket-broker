package com.alibaba.spring.boot.rsocket.demo;

import com.alibaba.rsocket.RSocketService;
import com.alibaba.user.Rx3UserService;
import com.alibaba.user.User;
import com.github.javafaker.Faker;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.springframework.stereotype.Service;

/**
 * Rx3UserService implementation
 *
 * @author linux_china
 */
@RSocketService(serviceInterface = Rx3UserService.class)
@Service
public class Rx3UserServiceImpl implements Rx3UserService {
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
    public Flowable<User> findAllPeople(String type) {
        return Flowable.range(0, 10)
                .map(id -> new User(id, "nick:" + type));
    }
}
