package org.jetbrains.bsp.protocol.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class StringUtils {
    private StringUtils() {
    }

    private static final ThreadLocal<MessageDigest> MD5 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });

    /**
     * Creates a md5 hashed string from an original string
     *
     * @param s the original string
     * @param n the number of characters taken from the hashed string
     */
    public static String md5Hash(String s, int n) {
        MessageDigest md5 = MD5.get();
        md5.reset();
        byte[] hash = md5.digest(s.getBytes(StandardCharsets.UTF_8));
        String hex = HexFormat.of().formatHex(hash, 0, n / 2 + n % 2);
        return hex.substring(0, Math.min(n, hex.length()));
    }
}
