package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * checks the crypto against the numbers its standards publish, then walks the handshake a
 * game's online mode does.
 */
public final class CryptoProgram implements Program {

    @Override
    public String id() {
        return "crypto";
    }

    @Override
    public String description() {
        return "checks the digests and aes against their published answers, then does a handshake";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();

        digests(console);
        aes(console);
        handshake(console);
        refusals(console);
    }

    private void refusals(ConsoleSink console) {
        try {
            new java.net.DatagramSocket(0);
            console.info("crypto: a datagram socket opened, which cannot happen");
        } catch (java.net.SocketException refused) {
            console.info("crypto: a datagram socket is refused by name");
        }
        try {
            new java.io.ObjectOutputStream(new java.io.ByteArrayOutputStream());
            console.info("crypto: an object stream opened, which cannot happen");
        } catch (java.io.IOException refused) {
            console.info("crypto: java serialisation is refused by name");
        }
    }

    private void digests(ConsoleSink console) throws Exception {
        byte[] abc = "abc".getBytes(StandardCharsets.UTF_8);
        console.info("crypto: sha-1 of abc is " + hex(digest("SHA-1", abc)));
        console.info("crypto: sha-256 of abc is " + hex(digest("SHA-256", abc)));
        console.info("crypto: md5 of abc is " + hex(digest("MD5", abc)));
        console.info("crypto: sha-1 of nothing is " + hex(digest("SHA-1", new byte[0])));

        MessageDigest running = MessageDigest.getInstance("SHA-256");
        for (byte value : "abc".getBytes(StandardCharsets.UTF_8)) {
            running.update(value);
        }
        console.info("crypto: a digest fed one byte at a time matches "
                + hex(running.digest()).equals(hex(digest("SHA-256", abc))));

        byte[] long1 = new byte[1000000];
        java.util.Arrays.fill(long1, (byte) 'a');
        console.info("crypto: sha-1 of a million a's is " + hex(digest("SHA-1", long1)));
    }

    private void aes(ConsoleSink console) throws Exception {
        byte[] key = bytes("000102030405060708090a0b0c0d0e0f");
        byte[] block = bytes("00112233445566778899aabbccddeeff");
        Cipher stream = Cipher.getInstance("AES/CFB8/NoPadding");
        stream.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                new IvParameterSpec(block));
        byte[] out = stream.update(new byte[] {0});
        console.info("crypto: the first aes keystream byte is " + hex(out));

        SecretKey secret = new SecretKeySpec(key, "AES");
        byte[] message = "a line the server would read".getBytes(StandardCharsets.UTF_8);
        Cipher writing = Cipher.getInstance("AES/CFB8/NoPadding");
        writing.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(key));
        Cipher reading = Cipher.getInstance("AES/CFB8/NoPadding");
        reading.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(key));

        byte[] half = writing.update(message, 0, 10);
        byte[] rest = writing.update(message, 10, message.length - 10);
        byte[] back = new byte[message.length];
        System.arraycopy(reading.update(half), 0, back, 0, half.length);
        System.arraycopy(reading.update(rest), 0, back, half.length, rest.length);
        console.info("crypto: a split stream still reads back '"
                + new String(back, StandardCharsets.UTF_8) + "'");
    }

    /** what a client and server do to agree on a key: rsa out, aes back */
    private void handshake(ConsoleSink console) throws Exception {
        KeyPairGenerator maker = KeyPairGenerator.getInstance("RSA");
        maker.initialize(1024);
        KeyPair server = maker.generateKeyPair();

        byte[] handedOut = server.getPublic().getEncoded();
        PublicKey read = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(handedOut));
        console.info("crypto: a 1024 bit key writes itself as " + handedOut.length
                + " X.509 bytes and reads back " + java.util.Arrays.equals(handedOut,
                        read.getEncoded()));

        KeyGenerator keys = KeyGenerator.getInstance("AES");
        keys.init(128);
        SecretKey shared = keys.generateKey();

        Cipher out = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        out.init(Cipher.ENCRYPT_MODE, read);
        byte[] sealed = out.doFinal(shared.getEncoded());

        Cipher in = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        in.init(Cipher.DECRYPT_MODE, server.getPrivate());
        byte[] opened = in.doFinal(sealed);
        console.info("crypto: the shared key arrives whole "
                + java.util.Arrays.equals(shared.getEncoded(), opened)
                + " in " + sealed.length + " sealed bytes");

        byte[] signed = "a file a launcher checks".getBytes(StandardCharsets.UTF_8);
        Signature signing = Signature.getInstance("SHA1withRSA");
        signing.initSign(server.getPrivate());
        signing.update(signed);
        byte[] mark = signing.sign();

        Signature checking = Signature.getInstance("SHA1withRSA");
        checking.initVerify(server.getPublic());
        checking.update(signed);
        boolean good = checking.verify(mark);

        Signature wrong = Signature.getInstance("SHA1withRSA");
        wrong.initVerify(server.getPublic());
        wrong.update("a file nobody signed".getBytes(StandardCharsets.UTF_8));
        console.info("crypto: a signature checks out " + good
                + " and a changed message does not " + !wrong.verify(mark));
    }

    private static byte[] digest(String algorithm, byte[] message) throws Exception {
        return MessageDigest.getInstance(algorithm).digest(message);
    }

    private static byte[] bytes(String written) {
        byte[] out = new byte[written.length() / 2];
        for (int at = 0; at < out.length; at++) {
            out[at] = (byte) Integer.parseInt(written.substring(at * 2, at * 2 + 2), 16);
        }
        return out;
    }

    private static String hex(byte[] bytes) {
        StringBuilder written = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            written.append(String.format(Locale.ROOT, "%02x", value));
        }
        return written.toString();
    }
}
