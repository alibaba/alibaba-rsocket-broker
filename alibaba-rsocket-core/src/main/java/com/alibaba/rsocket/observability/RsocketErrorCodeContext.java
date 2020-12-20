package com.alibaba.rsocket.observability;

import com.kevinten.vrml.error.code.ErrorCodeContext;

/**
 * The Rsocket error code context.
 */
public interface RsocketErrorCodeContext extends ErrorCodeContext {

    /**
     * The RsocketErrorCode GENERATOR.
     */
    RsocketErrorCodeGenerator GENERATOR = new RsocketErrorCodeGenerator();
    /**
     * The RsocketErrorCode MANAGER.
     */
    RsocketErrorCodeManager MANAGER = new RsocketErrorCodeManager();

    /**
     * The Rsocket error code generator.
     */
    class RsocketErrorCodeGenerator implements ErrorCodeGenerator {

        /**
         * Rsocket system code.
         *
         * @apiNote code like {@code RST-xxxxx}.
         */
        private static final String DEFAULT_APPLICATION_CODE = "RST-";

        @Override
        public String createErrorCode(String prefix, String code) {
            return applicationErrorCode() + prefix + code;
        }

        @Override
        public String applicationErrorCode() {
            return DEFAULT_APPLICATION_CODE;
        }
    }

    /**
     * The Rsocket error code manager.
     */
    class RsocketErrorCodeManager implements ErrorCodeManager<RsocketErrorCodeContext> {

        @Override
        public void showErrorCodeItem(RsocketErrorCodeContext errorCodeContext) {
            System.out.printf("%70s  %5s  %s", errorCodeContext.name(), errorCodeContext.getCode(), errorCodeContext.getMessage());
        }
    }
}
