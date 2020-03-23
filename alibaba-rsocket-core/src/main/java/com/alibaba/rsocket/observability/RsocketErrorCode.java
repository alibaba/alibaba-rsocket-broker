package com.alibaba.rsocket.observability;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * RSocket Error Code
 *
 * @author leijuan
 */
public class RsocketErrorCode {
    /**
     * bundle FQN
     */
    @NonNls
    private static final String BUNDLE_FQN = "rsocket.Messages";

    /**
     * resource bundle
     */
    private static final ResourceBundle ourBundle = ResourceBundle.getBundle(BUNDLE_FQN, new Locale("en", "US"));

    private RsocketErrorCode() {
    }

    /**
     * get value from resource bundle
     *
     * @param key    message key
     * @param params params，MessageFormat style
     * @return message value
     */
    public static String message(@PropertyKey(resourceBundle = BUNDLE_FQN) String key, Object... params) {
        String value;
        try {
            value = ourBundle.getString(key);
        } catch (MissingResourceException ignore) {
            value = "!!!" + key + "!!!";
        }
        if (params != null && params.length > 0 && value.indexOf('{') >= 0) {
            value = MessageFormat.format(value, params);
        }
        return key + ": " + value;
    }
}
