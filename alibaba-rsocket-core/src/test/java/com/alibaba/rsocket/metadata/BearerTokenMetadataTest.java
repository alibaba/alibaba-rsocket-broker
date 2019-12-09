package com.alibaba.rsocket.metadata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Bearer token metadata test
 *
 * @author leijuan
 */
public class BearerTokenMetadataTest {

    @Test
    public void testEncodingAndDecoding() {
        String token = "123456";
        BearerTokenMetadata tokenMetadata = BearerTokenMetadata.jwt(token);
        Assertions.assertEquals(token.length() + 1, tokenMetadata.getContent().capacity());
        BearerTokenMetadata tokenMetadata1 = BearerTokenMetadata.from(tokenMetadata.getContent());
        Assertions.assertEquals(tokenMetadata1.getBearerToken(), token);
    }
}
