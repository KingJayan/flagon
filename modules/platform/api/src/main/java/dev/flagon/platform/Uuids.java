package dev.flagon.platform;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public final class Uuids {

    private Uuids() {
    }

    public static UUID of(long high, long low) {
        return UUID.fromString(text(high, low));
    }

    /** the jdk's reading: five dashed hex fields, each masked to its width */
    public static UUID fromString(String text) {
        String[] fields = text.split("-", -1);
        if (fields.length != 5 || text.length() > 36) {
            throw new IllegalArgumentException("Invalid UUID string: " + text);
        }
        long[] values = new long[5];
        for (int at = 0; at < 5; at++) {
            if (fields[at].isEmpty()) {
                throw new IllegalArgumentException("Invalid UUID string: " + text);
            }
            values[at] = Long.parseLong(fields[at], 16);
        }
        long high = (values[0] & 0xffffffffL) << 32 | (values[1] & 0xffffL) << 16
                | values[2] & 0xffffL;
        long low = (values[3] & 0xffffL) << 48 | values[4] & 0xffffffffffffL;
        return of(high, low);
    }

    public static long getMostSignificantBits(UUID id) {
        String plain = plain(id);
        return parse(plain.substring(0, 16));
    }

    public static long getLeastSignificantBits(UUID id) {
        String plain = plain(id);
        return parse(plain.substring(16, 32));
    }

    public static UUID nameUUIDFromBytes(byte[] name) {
        byte[] digest = md5(name);
        digest[6] = (byte) ((digest[6] & 0x0f) | 0x30);
        digest[8] = (byte) ((digest[8] & 0x3f) | 0x80);
        long high = 0;
        long low = 0;
        for (int at = 0; at < 8; at++) {
            high = (high << 8) | (digest[at] & 0xffL);
            low = (low << 8) | (digest[at + 8] & 0xffL);
        }
        return of(high, low);
    }

    private static byte[] md5(byte[] name) {
        try {
            return MessageDigest.getInstance("MD5").digest(name);
        } catch (NoSuchAlgorithmException noMd5) {
            throw new IllegalStateException("md5 is missing from this bundle", noMd5);
        }
    }

    private static String text(long high, long low) {
        String plain = hex(high) + hex(low);
        return plain.substring(0, 8) + "-" + plain.substring(8, 12) + "-"
                + plain.substring(12, 16) + "-" + plain.substring(16, 20) + "-"
                + plain.substring(20);
    }

    private static String hex(long half) {
        String digits = Long.toHexString(half);
        return "0".repeat(16 - digits.length()) + digits;
    }

    private static String plain(UUID id) {
        return id.toString().replace("-", "");
    }

    private static long parse(String digits) {
        long half = 0;
        for (int at = 0; at < digits.length(); at++) {
            half = (half << 4) | Character.digit(digits.charAt(at), 16);
        }
        return half;
    }
}
