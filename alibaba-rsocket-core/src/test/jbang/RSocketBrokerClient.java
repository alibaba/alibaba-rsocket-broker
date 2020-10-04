//usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.slf4j:slf4j-simple:1.7.30
//DEPS org.projectlombok:lombok:1.18.12
//DEPS com.alibaba.rsocket:alibaba-rsocket-core:1.0.0.RC2

import com.alibaba.rsocket.encoding.JsonUtils;
import com.alibaba.rsocket.invocation.RSocketRemoteServiceBuilder;
import com.alibaba.rsocket.metadata.RSocketMimeType;
import com.alibaba.rsocket.upstream.UpstreamCluster;
import com.alibaba.rsocket.upstream.UpstreamManager;
import com.alibaba.rsocket.upstream.UpstreamManagerImpl;
import com.alibaba.rsocket.utils.RSocketRequesterSupportMock;
import lombok.Data;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * jbang RSocket Broker Client
 *
 * @author leijuan
 */
public class RSocketBrokerClient {
    private String jwtToken = "";
    private List<String> brokers = Collections.singletonList("tcp://127.0.0.1:9999");
    private UpstreamManager upstreamManager;

    public static void main(String[] args) throws Exception {
        RSocketBrokerClient client = new RSocketBrokerClient();
        UserService userService = client.userService();
        User user = userService.findById(1).block();
        System.out.println(JsonUtils.toJsonText(user));
        client.dispose();
    }

    public RSocketBrokerClient() throws Exception {
        initUpstreamManager();
    }

    public UserService userService() {
        return RSocketRemoteServiceBuilder
                .client(UserService.class)
                .service("com.alibaba.user.UserService")
                .encodingType(RSocketMimeType.Json)
                .acceptEncodingType(RSocketMimeType.Json)
                .upstreamManager(this.upstreamManager)
                .build();
    }

    public void dispose() {
        this.upstreamManager.close();
    }

    public void initUpstreamManager() throws Exception {
        this.upstreamManager = new UpstreamManagerImpl(new RSocketRequesterSupportMock(this.jwtToken, this.brokers));
        upstreamManager.add(new UpstreamCluster(null, "*", null, this.brokers));
        upstreamManager.init();
    }

    public interface UserService {
        Mono<User> findById(Integer id);

        Mono<String> getAppName();
    }

    @Data
    public static class User implements Serializable {
        private Integer id;
        private String nick;
        private String email;
        private String phone;
    }
}
