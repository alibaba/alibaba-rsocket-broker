package com.alibaba.rsocket.invocation;

import io.netty.buffer.ByteBuf;
import org.reactivestreams.Publisher;

/**
 * RSocket MIME type handler, such as application/graphql
 *
 * @author leijuan
 */
public interface RSocketMimeTypeHandler {
    /**
     * accepted content type
     *
     * @return content type
     */
    String contentType();

    /**
     * @param serviceName service name
     * @param methodName  method name
     * @param body        body
     * @return Mono or Flux result
     */
    Publisher<ByteBufResponse> invoke(String serviceName, String methodName, ByteBuf body);

    class ByteBufResponse {
        private String contentType;
        private ByteBuf data;

        public ByteBufResponse() {
        }

        public ByteBufResponse(String contentType, ByteBuf data) {
            this.contentType = contentType;
            this.data = data;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public ByteBuf getData() {
            return data;
        }

        public void setData(ByteBuf data) {
            this.data = data;
        }
    }
}
