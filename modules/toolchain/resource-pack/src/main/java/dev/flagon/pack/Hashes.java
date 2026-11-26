package dev.flagon.pack;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class Hashes {

    private Hashes() {
    }

    static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException(missing);
        }
    }
}
