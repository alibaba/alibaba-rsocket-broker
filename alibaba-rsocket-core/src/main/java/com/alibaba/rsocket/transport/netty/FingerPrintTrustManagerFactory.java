package com.alibaba.rsocket.transport.netty;

import io.netty.handler.ssl.util.SimpleTrustManagerFactory;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import java.security.KeyStore;
import java.util.List;

/**
 * Fingerprints trust manager factory
 *
 * @author leijuan
 */
public class FingerPrintTrustManagerFactory extends SimpleTrustManagerFactory {
    private TrustManager trustManager;

    public FingerPrintTrustManagerFactory(List<String> fingerPrintsSha256) {
        this.trustManager = new FingerPrintX509TrustManager(fingerPrintsSha256);
    }

    @Override
    protected void engineInit(KeyStore keyStore) throws Exception {

    }

    @Override
    protected void engineInit(ManagerFactoryParameters managerFactoryParameters) throws Exception {

    }

    @Override
    protected TrustManager[] engineGetTrustManagers() {
        return new TrustManager[]{trustManager};
    }

}
