package com.alibaba.rsocket.gateway.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JWT authentication service implementation
 *
 * @author leijuan
 */
@Component
public class JwtAuthenticationServiceImpl implements JwtAuthenticationService {
    private List<JWTVerifier> verifiers = new ArrayList<>();
    private static String iss = "RSocketBroker";
    /**
     * cache verified principal
     */
    Cache<Integer, NamedPrincipal> jwtVerifyCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    public JwtAuthenticationServiceImpl() throws Exception {
        File keyFile = new File(System.getProperty("user.home"), ".rsocket/jwt_rsa.pub");
        if (keyFile.exists()) {
            Algorithm algorithmRSA256Public = Algorithm.RSA256(readPublicKey(keyFile), null);
            this.verifiers.add(JWT.require(algorithmRSA256Public).withIssuer(iss).build());
        }
    }

    @Override
    public @Nullable NamedPrincipal auth(String jwtToken) {
        int tokenHashCode = jwtToken.hashCode();
        NamedPrincipal principal = jwtVerifyCache.getIfPresent(tokenHashCode);
        if (principal == null) {
            for (JWTVerifier verifier : verifiers) {
                try {
                    DecodedJWT decodedJWT = verifier.verify(jwtToken);
                    principal = new NamedPrincipal(decodedJWT.getSubject());
                    jwtVerifyCache.put(tokenHashCode, principal);
                    break;
                } catch (JWTVerificationException ignore) {

                }
            }
        }
        return principal;
    }

    public RSAPublicKey readPublicKey(File keyFile) throws Exception {
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

}
