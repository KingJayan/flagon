package dev.flagon.sandbox;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * the crypto a page must match. the jvm test checks these lines are what the jdk says, and the
 * browser test checks a bundle says the same.
 */
public final class CryptoChecks {

    private static final HexFormat HEX = HexFormat.of();

    private static final byte[] FOX =
            "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

    /** a p-256 public key and its SHA256withECDSA signature of "flagon checks", made by the jdk */
    private static final String JDK_EC_KEY =
            "3059301306072a8648ce3d020106082a8648ce3d03010703420004eb8aa31b33231db9e4659534b7"
            + "dbaac815fa4c63ae9276ba861e058bc744263a712b1f333f0846d4ee1a5e5ad700d4007a6da57683"
            + "4f0744e6e98ee783322494";

    private static final String JDK_EC_SIGNATURE =
            "304502210097fa7cf8914b1e9b4ad8f61fc0b502a34349155c3a96936ee1de913e6af10d3e02204b"
            + "d8d874b17e0912184d0f981e17a6eb42805181b511d60b1135a6a2672d5196";

    /** a 1024 bit rsa private key the jdk made, whose signatures do not change */
    private static final String JDK_RSA_KEY =
            "30820276020100300d06092a864886f70d0101010500048202603082025c02010002818100a132d4"
            + "071ef397f1ab66b36d0d2521737585594522e06f213b526ecbb6c56e307c3310c36f90aeaa0b7ad1"
            + "fa344d39237a91d50b25077bf030f6a42ad469f191c9af6a972db0843c40425c4ca64ad6454ff4b4"
            + "6830cd0f45e4a10e21c55dd39493f8b6871413d57b141204277654b318aa2fb36113270da356251a"
            + "a9a770a75f02030100010281803b9d0cfd6f2c5dc2418573107ebe7f0ef064b6bd532715e1ffcabd"
            + "efb7716f0d1e3952573c5090cf90fc446017ec811786521d2c086f2a9e0ab583c3d51732ff021b41"
            + "1d9f0f0fbd8a21f6175ded0e1f255b4f4217c6e62b07bb9f1a37454753eebe834f387b22a56b5476"
            + "185652c29c50dcef60ccae163b7d18fa6612f65f55024100fb7dec992c6bc965da090c068b865bac"
            + "b2198835cfc6b279b6d129b34c57890b05f4e00c5d1d6415ceb5acb65ae23591bb41038c0337a484"
            + "0d5dd61c88e4fd75024100a4168e236fd8a1145f0ea212819446ab1b4d988b22d10dc0d49c06c863"
            + "584da3f739625e2272f2b264d54b630326faa943123cea089c7abd63057f4cbdea130302405ec2c6"
            + "01b9412bf3dac209ebf56abce4477f6359935eb52533838088f4bd0f003e505493940a5d09f28775"
            + "fa8828303327318a1a99a3bfb39a2dbf5da51f5379024042f7bd9cf2d9cbabdfbbbe23a564d98e9d"
            + "bf0681ca7cdd8b147a753e3565659284a493e3ed55527dc22f56ec1aee3ea0f3f1ae90a9610baadd"
            + "8df1cd6538f1f70241008e17c55c563f1e2f28b5a3f48f2ee4688e833240b7ca605b08c94cc2e405"
            + "e03c672e781c2d0c9dd634046ad70a7665e898794a6c2914bd813fb7354e7aa00fa8";

    private static final String[] CURVES = {"secp256r1", "secp384r1", "secp521r1"};

    private interface Check {
        void run(List<String> out) throws Exception;
    }

    private CryptoChecks() {
    }

    public static List<String> run() {
        List<String> out = new ArrayList<>();
        check(out, "digest", CryptoChecks::digests);
        check(out, "hmac", CryptoChecks::hmacs);
        check(out, "pbkdf2", CryptoChecks::pbkdf2);
        check(out, "curve", CryptoChecks::curves);
        check(out, "eckey", CryptoChecks::ecKeys);
        check(out, "ecdh", CryptoChecks::ecdh);
        check(out, "ecdsa", CryptoChecks::ecdsa);
        check(out, "rsa", CryptoChecks::rsa);
        return out;
    }

    private static void check(List<String> out, String name, Check check) {
        List<String> lines = new ArrayList<>();
        try {
            check.run(lines);
        } catch (Exception failed) {
            lines.add("failed with " + failed);
        }
        for (String line : lines) {
            out.add(name + ": " + line);
        }
    }

    private static void digests(List<String> out) throws Exception {
        byte[] abc = "abc".getBytes(StandardCharsets.UTF_8);
        for (String name : new String[] {"SHA-384", "SHA-512"}) {
            out.add(name.toLowerCase(java.util.Locale.ROOT) + " of abc "
                    + HEX.formatHex(MessageDigest.getInstance(name).digest(abc)));
        }
        byte[] long1 = new byte[1000];
        Arrays.fill(long1, (byte) 'a');
        MessageDigest split = MessageDigest.getInstance("SHA-512");
        for (int at = 0; at < long1.length; at += 111) {
            split.update(long1, at, Math.min(111, long1.length - at));
        }
        out.add("sha-512 of 1000 a's fed apart " + HEX.formatHex(split.digest()));
    }

    private static void hmacs(List<String> out) throws Exception {
        byte[] key = "key".getBytes(StandardCharsets.UTF_8);
        for (String name : new String[] {"HmacMD5", "HmacSHA1", "HmacSHA256", "HmacSHA384",
            "HmacSHA512"}) {
            Mac mac = Mac.getInstance(name);
            mac.init(new SecretKeySpec(key, name));
            out.add(name + " " + mac.getMacLength() + " " + HEX.formatHex(mac.doFinal(FOX)));
        }
        byte[] longKey = new byte[200];
        Arrays.fill(longKey, (byte) 0xAA);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(longKey, "HmacSHA256"));
        mac.update(FOX, 0, 10);
        Mac twin = (Mac) mac.clone();
        mac.update(FOX, 10, FOX.length - 10);
        byte[] whole = mac.doFinal();
        twin.update(FOX, 10, FOX.length - 10);
        byte[] into = new byte[40];
        twin.doFinal(into, 8);
        out.add("a long key " + HEX.formatHex(whole) + " clones "
                + Arrays.equals(whole, Arrays.copyOfRange(into, 8, 40)));
        mac.update((byte) 1);
        mac.reset();
        out.add("after a reset " + Arrays.equals(whole, mac.doFinal(FOX)));
        try {
            Mac.getInstance("HmacSHA1").update(FOX);
            out.add("an unready mac took bytes");
        } catch (IllegalStateException refused) {
            out.add("an unready mac says " + refused.getMessage());
        }
    }

    private static void pbkdf2(List<String> out) throws Exception {
        derive(out, "PBKDF2WithHmacSHA1", "password", "salt", 2, 160);
        derive(out, "PBKDF2WithHmacSHA256", "password", "salt", 4096, 256);
        derive(out, "PBKDF2WithHmacSHA512", "passwordPASSWORDpassword",
                "saltSALTsaltSALTsaltSALTsaltSALTsalt", 1000, 600);
        derive(out, "PBKDF2WithHmacSHA256", "", "salt", 1, 128);
    }

    private static void derive(List<String> out, String name, String password, String salt,
                               int rounds, int bits) throws Exception {
        SecretKey key = SecretKeyFactory.getInstance(name).generateSecret(new PBEKeySpec(
                password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), rounds, bits));
        out.add(name + " " + rounds + " " + key.getAlgorithm() + " " + key.getFormat() + " "
                + HEX.formatHex(key.getEncoded()));
    }

    private static void curves(List<String> out) throws Exception {
        for (String name : CURVES) {
            ECParameterSpec params = params(name);
            MessageDigest all = MessageDigest.getInstance("SHA-256");
            for (BigInteger part : new BigInteger[] {
                ((ECFieldFp) params.getCurve().getField()).getP(), params.getCurve().getA(),
                params.getCurve().getB(), params.getGenerator().getAffineX(),
                params.getGenerator().getAffineY(), params.getOrder()}) {
                all.update(part.toByteArray());
            }
            out.add(name + " " + params.getCurve().getField().getFieldSize() + " bits, cofactor "
                    + params.getCofactor() + ", numbers " + HEX.formatHex(all.digest()));
        }
        KeyPair standard = KeyPairGenerator.getInstance("EC").generateKeyPair();
        out.add("a plain generator makes " + ((ECPublicKey) standard.getPublic()).getParams()
                .getOrder().bitLength() + " bit keys");
        try {
            KeyPairGenerator.getInstance("EC").initialize(new ECGenParameterSpec("secp256k1"));
            out.add("secp256k1 was taken");
        } catch (java.security.InvalidAlgorithmParameterException refused) {
            out.add("secp256k1 is refused");
        }
    }

    private static void ecKeys(List<String> out) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("EC");
        for (String name : CURVES) {
            KeyPair pair = pair(name);
            byte[] x509 = pair.getPublic().getEncoded();
            byte[] pkcs8 = pair.getPrivate().getEncoded();
            PublicKey readPublic = factory.generatePublic(new X509EncodedKeySpec(x509));
            PrivateKey readPrivate = factory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            ECPublicKey ec = (ECPublicKey) pair.getPublic();
            PublicKey fromPoint = factory.generatePublic(
                    new ECPublicKeySpec(ec.getW(), ec.getParams()));
            out.add(name + " " + pair.getPublic().getAlgorithm() + " "
                    + pair.getPublic().getFormat() + " " + x509.length + " bytes starting "
                    + HEX.formatHex(x509, 0, x509.length - 2 * ((ec.getParams().getOrder()
                    .bitLength() + 7) / 8)) + ", " + pair.getPrivate().getFormat() + " "
                    + pkcs8.length + " bytes, read back " + readPublic.equals(pair.getPublic())
                    + " " + readPrivate.equals(pair.getPrivate()) + " " + fromPoint.equals(ec));
        }
    }

    private static void ecdh(List<String> out) throws Exception {
        for (String name : CURVES) {
            ECParameterSpec params = params(name);
            KeyFactory factory = KeyFactory.getInstance("EC");
            BigInteger secret = new BigInteger(1, MessageDigest.getInstance("SHA-256")
                    .digest(name.getBytes(StandardCharsets.UTF_8)));
            PrivateKey fixed = factory.generatePrivate(new ECPrivateKeySpec(secret, params));
            PublicKey generator = factory.generatePublic(
                    new ECPublicKeySpec(params.getGenerator(), params));
            out.add(name + " fixed secret times the generator " + HEX.formatHex(agree(fixed,
                    generator)));
            KeyPair alice = pair(name);
            KeyPair bob = pair(name);
            byte[] one = agree(alice.getPrivate(), bob.getPublic());
            byte[] other = agree(bob.getPrivate(), alice.getPublic());
            out.add(name + " both sides agree " + Arrays.equals(one, other) + " on "
                    + one.length + " bytes");
        }
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        KeyPair pair = pair("secp256r1");
        agreement.init(pair.getPrivate());
        try {
            agreement.doPhase(pair.getPublic(), false);
        } catch (IllegalStateException refused) {
            out.add("a middle phase says " + refused.getMessage());
        }
        try {
            agreement.doPhase(pair("secp384r1").getPublic(), true);
            out.add("a key on another curve was taken");
        } catch (java.security.InvalidKeyException refused) {
            out.add("a key on another curve is refused");
        }
        agreement.doPhase(pair.getPublic(), true);
        try {
            agreement.generateSecret("AES");
        } catch (java.security.NoSuchAlgorithmException refused) {
            out.add("an aes secret says " + refused.getMessage());
        }
        SecretKey shared = agreement.generateSecret("TlsPremasterSecret");
        out.add("a shared key is " + shared.getAlgorithm() + " " + shared.getEncoded().length);
    }

    private static void ecdsa(List<String> out) throws Exception {
        String[][] uses = {{"secp256r1", "SHA256withECDSA"}, {"secp384r1", "SHA384withECDSA"},
            {"secp521r1", "SHA512withECDSA"}, {"secp256r1", "SHA1withECDSA"}};
        byte[] message = "flagon checks".getBytes(StandardCharsets.UTF_8);
        for (String[] use : uses) {
            KeyPair pair = pair(use[0]);
            Signature signer = Signature.getInstance(use[1]);
            signer.initSign(pair.getPrivate());
            signer.update(message);
            byte[] signature = signer.sign();
            Signature checker = Signature.getInstance(use[1]);
            checker.initVerify(pair.getPublic());
            checker.update(message);
            boolean good = checker.verify(signature);
            checker.update("flagon checkS".getBytes(StandardCharsets.UTF_8));
            out.add(use[1] + " on " + use[0] + " checks out " + good + ", a changed message "
                    + checker.verify(signature) + ", a der pair " + (signature[0] == 0x30));
        }
        PublicKey jdk = KeyFactory.getInstance("EC").generatePublic(
                new X509EncodedKeySpec(HEX.parseHex(JDK_EC_KEY)));
        Signature checker = Signature.getInstance("SHA256withECDSA");
        checker.initVerify(jdk);
        checker.update(message);
        out.add("a jdk signature checks out " + checker.verify(HEX.parseHex(JDK_EC_SIGNATURE)));
        checker.update(message);
        try {
            checker.verify(new byte[] {1, 2, 3});
            out.add("garbage passed for a signature");
        } catch (java.security.SignatureException refused) {
            out.add("garbage is refused: " + refused.getMessage());
        }
    }

    private static void rsa(List<String> out) throws Exception {
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(HEX.parseHex(JDK_RSA_KEY)));
        for (String name : new String[] {"SHA384withRSA", "SHA512withRSA"}) {
            Signature signer = Signature.getInstance(name);
            signer.initSign(key);
            signer.update(FOX);
            out.add(name + " " + HEX.formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(signer.sign())));
        }
    }

    private static ECParameterSpec params(String name) throws Exception {
        return ((ECPublicKey) pair(name).getPublic()).getParams();
    }

    private static KeyPair pair(String name) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(name));
        return generator.generateKeyPair();
    }

    private static byte[] agree(PrivateKey mine, PublicKey theirs) throws Exception {
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(mine);
        agreement.doPhase(theirs, true);
        return agreement.generateSecret();
    }
}
