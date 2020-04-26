package com.alibaba.spring.boot.rsocket.broker.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * authentication service with JWT implementation, please refer https://github.com/auth0/java-jwt
 *
 * @author leijuan
 */
@Service
public class AuthenticationServiceJwtImpl implements AuthenticationService {
    private List<JWTVerifier> verifiers = new ArrayList<>();
    private static String iss = "RSocketBroker";
    /**
     * cache verified principal
     */
    Cache<Integer, RSocketAppPrincipal> jwtVerifyCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    public AuthenticationServiceJwtImpl() throws Exception {
        File rsocketKeysDir = new File(System.getProperty("user.home"), ".rsocket");
        File publicKeyFile = new File(rsocketKeysDir, "jwt_rsa.pub");
        // generate RSA key pairs automatically
        if (!publicKeyFile.exists()) {
            if (!rsocketKeysDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                rsocketKeysDir.mkdir();
            }
            generateRSAKeyPairs(rsocketKeysDir);
        }
        Algorithm algorithmRSA256Public = Algorithm.RSA256(readPublicKey(), null);
        this.verifiers.add(JWT.require(algorithmRSA256Public).withIssuer(iss).build());
    }

    @Override
    @Nullable
    public RSocketAppPrincipal auth(String type, String credentials) {
        int tokenHashCode = credentials.hashCode();
        RSocketAppPrincipal principal = jwtVerifyCache.getIfPresent(tokenHashCode);
        for (JWTVerifier verifier : verifiers) {
            try {
                principal = new JwtPrincipal(verifier.verify(credentials), credentials);
                jwtVerifyCache.put(tokenHashCode, principal);
                break;
            } catch (JWTVerificationException ignore) {

            }
        }
        return principal;
    }

    public String generateCredentials(String id, String[] organizations, String[] serviceAccounts, String[] roles, String[] authorities, String sub, String[] audience) throws Exception {
        Algorithm algorithmRSA256Private = Algorithm.RSA256(null, readPrivateKey());
        Arrays.sort(audience);
        Arrays.sort(organizations);
        JWTCreator.Builder builder = JWT.create()
                .withIssuer(iss)
                .withSubject(sub)
                .withAudience(audience)
                .withIssuedAt(new Date())
                .withClaim("id", id)
                .withArrayClaim("sas", serviceAccounts)
                .withArrayClaim("orgs", organizations);
        if (roles != null && roles.length > 0) {
            Arrays.sort(roles);
            builder = builder.withArrayClaim("roles", roles);
        }
        if (authorities != null && authorities.length > 0) {
            builder = builder.withArrayClaim("authorities", authorities);
        }
        return builder.sign(algorithmRSA256Private);
    }


    public RSAPrivateKey readPrivateKey() throws Exception {
        File keyFile = new File(System.getProperty("user.home"), ".rsocket/jwt_rsa.key");
        try (InputStream inputStream = new FileInputStream(keyFile)) {
            byte[] keyBytes = toBytes(inputStream);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        }

    }

    public RSAPublicKey readPublicKey() throws Exception {
        File keyFile = new File(System.getProperty("user.home"), ".rsocket/jwt_rsa.pub");
        try (InputStream inputStream = new FileInputStream(keyFile)) {
            byte[] keyBytes = toBytes(inputStream);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        }
    }

    public byte[] toBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        StreamUtils.copy(inputStream, buffer);
        byte[] bytes = buffer.toByteArray();
        inputStream.close();
        buffer.close();
        return bytes;
    }

    private void generateRSAKeyPairs(File rsocketKeysDir) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        Key pub = keyPair.getPublic();
        Key pvt = keyPair.getPrivate();
        try (OutputStream out = new FileOutputStream(new File(rsocketKeysDir, "jwt_rsa.key"))) {
            out.write(pvt.getEncoded());
        }
        try (OutputStream out2 = new FileOutputStream(new File(rsocketKeysDir, "jwt_rsa.pub"))) {
            out2.write(pub.getEncoded());
        }
    }
}
