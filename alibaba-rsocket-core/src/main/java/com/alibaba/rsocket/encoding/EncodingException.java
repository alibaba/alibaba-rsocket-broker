package com.alibaba.rsocket.encoding;

import io.rsocket.exceptions.RSocketException;

/**
 * Encoding Exception
 *
 * @author linux_china
 */
public class EncodingException extends RSocketException {
    public EncodingException(String message) {
        super(message);
    }

    public EncodingException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int errorCode() {
        return 0x00000501;
    }
}
