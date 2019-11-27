package com.alibaba.spring.boot.rsocket.broker.security;

import com.alibaba.spring.boot.rsocket.broker.supporting.RSocketLocalService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
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

/**
 * authentication service with JWT implementation, please refer https://github.com/auth0/java-jwt
 *
 * @author leijuan
 */
@Service
public class AuthenticationServiceJwtImpl implements AuthenticationService {
    private List<JWTVerifier> verifiers = new ArrayList<>();
    private static String iss = "RSocketBroker";

    public AuthenticationServiceJwtImpl() throws Exception {
        File privateKeyFile = new File(System.getProperty("user.home"), ".rsocket/jwt_rsa.pub");
        if (!privateKeyFile.exists()) {
            generateRSAKeyPairs(System.getProperty("user.home") + "/.rsocket/jwt_rsa");
        }
        Algorithm algorithmRSA256Public = Algorithm.RSA256(readPublicKey(), null);
        this.verifiers.add(JWT.require(algorithmRSA256Public).withIssuer(iss).build());
    }

    @Override
    @Nullable
    public RSocketAppPrincipal auth(String type, String credentials) {
        for (JWTVerifier verifier : verifiers) {
            try {
                return new JwtPrincipal(verifier.verify(credentials), credentials);
            } catch (JWTVerificationException ignore) {

            }
        }
        return null;
    }

    public String generateCredentials(String[] organizations, String[] serviceAccounts, String[] roles, String sub, String[] audience) throws Exception {
        Algorithm algorithmRSA256Private = Algorithm.RSA256(null, readPrivateKey());
        Arrays.sort(roles);
        Arrays.sort(audience);
        Arrays.sort(organizations);
        return JWT.create()
                .withIssuer(iss)
                .withSubject(sub)
                .withAudience(audience)
                .withIssuedAt(new Date())
                .withArrayClaim("roles", roles)
                .withArrayClaim("sas", serviceAccounts)
                .withArrayClaim("orgs", organizations)
                .sign(algorithmRSA256Private);
    }


    public RSAPrivateKey readPrivateKey() throws Exception {
        File keyFile = new File(System.getProperty("user.home"), ".rsocket/jwt_rsa.key");
        byte[] keyBytes = toBytes(new FileInputStream(keyFile));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    public RSAPublicKey readPublicKey() throws Exception {
        File keyFile = new File(System.getProperty("user.home"), ".rsocket/jwt_rsa.pub");
        byte[] keyBytes = toBytes(new FileInputStream(keyFile));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    public byte[] toBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        StreamUtils.copy(inputStream, buffer);
        byte[] bytes = buffer.toByteArray();
        inputStream.close();
        buffer.close();
        return bytes;
    }

    private void generateRSAKeyPairs(String outFile) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        Key pub = keyPair.getPublic();
        Key pvt = keyPair.getPrivate();
        OutputStream out = new FileOutputStream(outFile + ".key");
        out.write(pvt.getEncoded());
        out.close();
        out = new FileOutputStream(outFile + ".pub");
        out.write(pub.getEncoded());
        out.close();
    }
}
