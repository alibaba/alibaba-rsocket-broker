package com.alibaba.rsocket.encoding.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectEncodingHandlerJsonImplTest {
    private ObjectEncodingHandlerJsonImpl objectEncodingHandlerJson = new ObjectEncodingHandlerJsonImpl();
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDecodeParamsForObject() throws Exception {
        UserDTO dto = new UserDTO("1", "demo");
        final byte[] bytes = objectMapper.writeValueAsBytes(dto);
        final ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        final Object o = objectEncodingHandlerJson.decodeParams(byteBuf, UserDTO.class);
        assertThat(dto).usingRecursiveComparison().isEqualTo(o);
    }

    @Test
    public void testDecodeParamsForList() throws Exception {
        UserDTO dto = new UserDTO("1", "demo");
        final List<UserDTO> dtos = Arrays.asList(dto);
        final byte[] bytes = objectMapper.writeValueAsBytes(dtos);
        final ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        final Object params = objectEncodingHandlerJson.decodeParams(byteBuf, UserDTO.class);
        Assertions.assertNotNull(params);
        Assertions.assertTrue(params.getClass().isArray());
    }

    public static class UserDTO {
        private String id;
        private String nick;

        public UserDTO() {
        }

        public UserDTO(String id, String nick) {
            this.id = id;
            this.nick = nick;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getNick() {
            return nick;
        }

        public void setNick(String nick) {
            this.nick = nick;
        }
    }
}
