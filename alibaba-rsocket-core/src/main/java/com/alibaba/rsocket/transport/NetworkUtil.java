package com.alibaba.rsocket.transport;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * Network Util
 *
 * @author leijuan
 */
public class NetworkUtil {
    /**
     * ip black list. 10.0.2.15 is default ip for virtual box vm
     */
    public static final List<String> IP_BLACK_LIST = Arrays.asList("10.0.2.15");
    public static String LOCAL_IP = getLocalIP();

    private static String getLocalIP() {
        String ip = null;
        try {
            Enumeration<?> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration<?> ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress inetAddress = (InetAddress) ee.nextElement();
                    String hostAddress = inetAddress.getHostAddress();
                    if (hostAddress.contains(".") && !IP_BLACK_LIST.contains(hostAddress) && !inetAddress.isLoopbackAddress()) {
                        ip = hostAddress;
                        break;
                    }
                }
            }
            if (ip == null) {
                ip = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (Exception ignore) {
            return "127.0.0.1";
        }
        return ip;
    }
}
