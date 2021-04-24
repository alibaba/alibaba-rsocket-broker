package com.alibaba.spring.boot.rsocket.broker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

/**
 * broker configuration
 *
 * @author leijuan
 */
@ConfigurationProperties(
        prefix = "rsocket.broker"
)
public class RSocketBrokerProperties {
    /**
     * listen port
     */
    private int listen = 9999;
    /**
     * topology: gossip, k8s, standalone
     */
    private String topology;
    /**
     * configuration store, such as h2://appsConfig.db, redis://localhost/0
     */
    private String configStore = "mem://demo";
    /**
     * external domain for requester from external: the requester can not access broker's internal ip
     */
    private String externalDomain;
    /**
     * auth required
     */
    private boolean authRequired = true;
    @NestedConfigurationProperty
    private RSocketSSL ssl;
    private List<String> upstreamBrokers;
    private String upstreamToken;

    public int getListen() {
        return listen;
    }

    public void setListen(int listen) {
        this.listen = listen;
    }

    public String getExternalDomain() {
        return externalDomain;
    }

    public void setExternalDomain(String externalDomain) {
        this.externalDomain = externalDomain;
    }

    public boolean isAuthRequired() {
        return authRequired;
    }

    public void setAuthRequired(boolean authRequired) {
        this.authRequired = authRequired;
    }

    public String getTopology() {
        return topology;
    }

    public void setTopology(String topology) {
        this.topology = topology;
    }

    public String getConfigStore() {
        return configStore;
    }

    public void setConfigStore(String configStore) {
        this.configStore = configStore;
    }

    public RSocketSSL getSsl() {
        return ssl;
    }

    public void setSsl(RSocketSSL ssl) {
        this.ssl = ssl;
    }

    public static class RSocketSSL {
        private boolean enabled = false;
        private String keyStoreType = "PKCS12";
        private String keyStore = System.getProperty("user.home") + "/.rsocket/rsocket.p12";
        private String keyStorePassword = "changeit";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getKeyStoreType() {
            return keyStoreType;
        }

        public void setKeyStoreType(String keyStoreType) {
            this.keyStoreType = keyStoreType;
        }

        public String getKeyStore() {
            return keyStore;
        }

        public void setKeyStore(String keyStore) {
            this.keyStore = keyStore;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        public void setKeyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }
    }

    public List<String> getUpstreamBrokers() {
        return upstreamBrokers;
    }

    public void setUpstreamBrokers(List<String> upstreamBrokers) {
        this.upstreamBrokers = upstreamBrokers;
    }

    public String getUpstreamToken() {
        return upstreamToken;
    }

    public void setUpstreamToken(String upstreamToken) {
        this.upstreamToken = upstreamToken;
    }
}
