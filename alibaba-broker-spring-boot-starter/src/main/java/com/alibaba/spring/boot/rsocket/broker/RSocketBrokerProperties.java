package com.alibaba.spring.boot.rsocket.broker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

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
    private int port = 9999;
    /**
     * topology: gossip, k8s, standalone
     */
    private String topology;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
}
