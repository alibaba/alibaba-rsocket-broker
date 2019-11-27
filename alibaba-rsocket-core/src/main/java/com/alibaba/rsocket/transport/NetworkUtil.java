package com.alibaba.rsocket.transport;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Network Util
 *
 * @author leijuan
 */
public class NetworkUtil {
    public static String getLocalIP() {
        String ip = null;
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress inetAddress = (InetAddress) ee.nextElement();
                    String hostAddress = inetAddress.getHostAddress();
                    if (hostAddress.contains(".") && !inetAddress.isLoopbackAddress()) {
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
