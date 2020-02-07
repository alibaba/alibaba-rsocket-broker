package com.alibaba.rsocket.events;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * config event
 *
 * @author leijuan
 */
public class ConfigEvent implements CloudEventSupport<ConfigEvent> {
    /**
     * config event logic id
     */
    private String id;
    private String appName;
    private String contentType;
    private String content;
    private String contentDigest;
    /**
     * occur timestamp, and unit is millis
     */
    private Long occurTimestamp;

    public ConfigEvent() {
        this.id = UUID.randomUUID().toString();
        this.occurTimestamp = System.currentTimeMillis();
    }

    public ConfigEvent(String appName, String contentType, String content) {
        this();
        this.appName = appName;
        this.contentType = contentType;
        this.content = content;
        this.contentDigest = sha256Hex(content);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentDigest() {
        return contentDigest;
    }

    public void setContentDigest(String contentDigest) {
        this.contentDigest = contentDigest;
    }

    public Long getOccurTimestamp() {
        return occurTimestamp;
    }

    public void setOccurTimestamp(Long occurTimestamp) {
        this.occurTimestamp = occurTimestamp;
    }

    public String sha256Hex(String text) {
        if (text == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ignore) {
            return "";
        }
    }
}
