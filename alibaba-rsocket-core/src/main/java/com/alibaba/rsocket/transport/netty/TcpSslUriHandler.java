/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.rsocket.transport.netty;

import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.uri.UriHandler;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * A SSL implementation of {@link UriHandler} that creates {@link TcpClientTransport}s and {@link TcpServerTransport}s.
 *
 * @author leijuan
 */
public final class TcpSslUriHandler implements UriHandler {
    private static final List<String> SCHEMES = Arrays.asList("tcps", "tcp+tls","tls");
    private static final String DEFAULT_PASSWORD = "changeit";
    private TrustManagerFactory trustManagerFactory = InsecureTrustManagerFactory.INSTANCE;
    private static final String[] protocols = new String[]{"TLSv1.3", "TLSv1.2"};

    public TcpSslUriHandler() {
        File fingerPrints = new File(System.getProperty("user.home") + "/.rsocket/known_finder_prints");
        if (fingerPrints.exists()) {
            List<String> fingerPrintsSha256 = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fingerPrints), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!line.isEmpty()) {
                        String fingerPrint = line.replaceAll(":", "");
                        fingerPrintsSha256.add(fingerPrint.trim());
                    }
                }
            } catch (Exception ignore) {

            }
            if (!fingerPrintsSha256.isEmpty()) {
                trustManagerFactory = new FingerPrintTrustManagerFactory(fingerPrintsSha256);
            }
        }
    }

    @Override
    public Optional<ClientTransport> buildClient(URI uri) {
        Objects.requireNonNull(uri, "uri must not be null");

        if (!SCHEMES.contains(uri.getScheme())) {
            return Optional.empty();
        }
        try {
            SslContext context = SslContextBuilder
                    .forClient()
                    .protocols(protocols)
                    .sslProvider(getSslProvider())
                    .trustManager(trustManagerFactory).build();
            TcpClient tcpClient = TcpClient.create()
                    .host(uri.getHost())
                    .port(uri.getPort())
                    .secure(ssl -> ssl.sslContext(context));
            return Optional.of(TcpClientTransport.create(tcpClient));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ServerTransport<?>> buildServer(URI uri) {
        Objects.requireNonNull(uri, "uri must not be null");
        if (!SCHEMES.contains(uri.getScheme())) {
            return Optional.empty();
        }
        try {
            Map<String, String> params = splitQuery(uri);
            PrivateKey privateKey;
            X509Certificate certificate;
            char[] password = params.getOrDefault("password", DEFAULT_PASSWORD).toCharArray();
            File keyStore = new File(params.getOrDefault("store", System.getProperty("user.home") + "/.rsocket/rsocket.p12"));
            if (keyStore.exists()) { // key store found
                KeyStore store = KeyStore.getInstance("PKCS12");
                try (InputStream is = new FileInputStream(keyStore)) {
                    store.load(is, password);
                }
                String alias = store.aliases().nextElement();
                certificate = (X509Certificate) store.getCertificate(alias);
                KeyStore.Entry entry = store.getEntry(alias, new KeyStore.PasswordProtection(password));
                privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            } else {  // user netty self signed certification
                SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
                privateKey = selfSignedCertificate.key();
                certificate = selfSignedCertificate.cert();
            }
            TcpServer tcpServer = TcpServer.create()
                    .host(uri.getHost())
                    .port(uri.getPort())
                    .secure(ssl -> ssl.sslContext(
                            SslContextBuilder.forServer(privateKey, certificate)
                                    .protocols(protocols)
                                    .sslProvider(getSslProvider())
                    ));
            return Optional.of(TcpServerTransport.create(tcpServer));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    private SslProvider getSslProvider() {
        if (OpenSsl.isAvailable()) {
            return SslProvider.OPENSSL_REFCNT;
        } else {
            return SslProvider.JDK;
        }
    }

    private Map<String, String> splitQuery(URI url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = url.getQuery();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
            }
        }
        return query_pairs;
    }
}
