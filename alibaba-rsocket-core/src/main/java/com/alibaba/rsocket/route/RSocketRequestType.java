package com.alibaba.rsocket.route;

/**
 * RSocket request type
 *
 * @author linux_china
 */
public enum RSocketRequestType {
    REQUEST_RESPONSE((byte) 0x04),
    REQUEST_FNF((byte) 0x05),
    REQUEST_STREAM((byte) 0x06),
    REQUEST_CHANNEL((byte) 0x07),
    PAYLOAD((byte) 0x0A),
    ERROR((byte) 0x0B);

    private byte id;

    RSocketRequestType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }
}
