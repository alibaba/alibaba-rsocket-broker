package com.alibaba.broker.example;

import com.alibaba.rsocket.RSocketAppContext;
import com.alibaba.rsocket.transport.NetworkUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.rsocket.RSocket;
import io.rsocket.metadata.WellKnownMimeType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;

import java.util.HashMap;
import java.util.Map;

/**
 * RSocketRequester call test
 *
 * @author leijuan
 */
public class RSocketRequesterCallTest {
    private static RSocketRequester rsocketRequester;
    private static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void setUp() throws Exception {
        RSocketStrategies rSocketStrategies = RSocketStrategies.builder()
                .encoder(new Jackson2JsonEncoder())
                .decoder(new Jackson2JsonDecoder())
                .build();
        rsocketRequester = RSocketRequester.builder()
                .dataMimeType(MimeType.valueOf(WellKnownMimeType.APPLICATION_JSON.getString()))
                .metadataMimeType(MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString()))
                // setup app info, json format as {"ip":"192.168.1.64","name":"MockApp","sdk":"SpringBoot/2.3.7","uuid":"f482220a-4d43-438c-b56a-281ca121c700","device":"JavaApp"}
                .setupMetadata(objectMapper.writeValueAsString(getAppMetadata()), MimeType.valueOf("message/x.rsocket.application+json"))
                // set up JWT token
                //.setupMetadata(AuthMetadataCodec.encodeBearerMetadata(ByteBufAllocator.DEFAULT, "jwt-token".toCharArray()), MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString()))
                .rsocketStrategies(rSocketStrategies)
                .connectTcp("localhost", 9999).block();
    }

    @AfterAll
    public static void tearDown() {
        RSocket rsocket = rsocketRequester.rsocket();
        rsocket.dispose();
    }


    @Test
    public void testUppercase() {
        String result = rsocketRequester.route("com.alibaba.WordService.uppercase")
                .data("[\"hello\"]")
                .retrieveMono(String.class).block();
        System.out.println(result);
    }

    @Test
    public void testAppJson() throws Exception {
        System.out.println(objectMapper.writeValueAsString(getAppMetadata()));
    }

    public static Map<String, Object> getAppMetadata() {
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("uuid", RSocketAppContext.ID);
        appMetadata.put("name", "MockApp");
        appMetadata.put("ip", NetworkUtil.LOCAL_IP);
        appMetadata.put("device", "JavaApp");
        appMetadata.put("sdk", "SpringBoot/2.3.7");
        return appMetadata;
    }
}
