package com.alibaba.rsocket.metadata;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.metadata.AuthMetadataCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bearer token metadata test
 *
 * @author leijuan
 */
public class BearerTokenMetadataTest {

    @Test
    public void testEncode() {
        String token = "123456";
        BearerTokenMetadata tokenMetadata = BearerTokenMetadata.jwt(token.toCharArray());
        Assertions.assertEquals(token.length() + 1, tokenMetadata.getContent().readableBytes());
        BearerTokenMetadata tokenMetadata1 = BearerTokenMetadata.from(tokenMetadata.getContent());
        Assertions.assertEquals(String.valueOf(tokenMetadata1.getBearerToken()), token);
        ByteBuf byteBuf = AuthMetadataCodec.encodeBearerMetadata(PooledByteBufAllocator.DEFAULT, token.toCharArray());
        assertThat(toArrayString(tokenMetadata.getContent())).isEqualTo(toArrayString(byteBuf));
    }

    @Test
    public void testDecode() {
        String token = "123456";
        ByteBuf byteBuf = AuthMetadataCodec.encodeBearerMetadata(PooledByteBufAllocator.DEFAULT, token.toCharArray()).duplicate();
        AuthMetadataCodec.readWellKnownAuthType(byteBuf);
        String token2 = new String(AuthMetadataCodec.readBearerTokenAsCharArray(byteBuf));
        assertThat(token).isEqualTo(token2);
    }

    private String toArrayString(ByteBuf byteBuf) {
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        return Arrays.toString(bytes);
    }
}
