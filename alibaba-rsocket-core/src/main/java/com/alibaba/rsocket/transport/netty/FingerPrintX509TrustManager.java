package com.alibaba.rsocket.transport.netty;


import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Certificate fingerprints trust manager
 *
 * @author leijuan
 */
public class FingerPrintX509TrustManager implements X509TrustManager {
    private List<String> fingerPrintsSha256;

    public FingerPrintX509TrustManager(List<String> fingerPrintsSha256) {
        this.fingerPrintsSha256 = new ArrayList<>();
        for (String fingerPrint : fingerPrintsSha256) {
            this.fingerPrintsSha256.add(fingerPrint.toUpperCase());
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String s) throws CertificateException {
        if (chain.length != 1) {
            throw new CertificateException("Expected exactly one certificate in the chain.");
        }
        chain[0].checkValidity();
        X509Certificate x509Cert = chain[0];
        String certFingerprint = getFingerprint("SHA-256", x509Cert);
        if (!fingerPrintsSha256.contains(certFingerprint.toUpperCase())) {
            throw new CertificateException("Invalid fingerprint: " + certFingerprint);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public static String getFingerprint(String algorithm, Certificate cert) {
        try {
            byte[] encCertInfo = cert.getEncoded();
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(encCertInfo);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                byte2hex(b, sb);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            // ignored
        }
        return "";
    }

    private static void byte2hex(byte b, StringBuilder buf) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]).append(hexChars[low]);
    }
}
