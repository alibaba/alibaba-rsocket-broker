package com.alibaba.rsocket.encoding;

/**
 * Encoding Exception
 *
 * @author linux_china
 */
public class EncodingException extends RuntimeException {

    public EncodingException(String message) {
        super(message);
    }

    public EncodingException(String message, Throwable cause) {
        super(message, cause);
    }

}
