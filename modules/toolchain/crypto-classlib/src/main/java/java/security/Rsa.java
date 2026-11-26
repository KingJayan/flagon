package java.security;

import java.math.BigInteger;
import java.util.Random;

public final class Rsa {

    public static final BigInteger EXPONENT = BigInteger.valueOf(65537);

    private Rsa() {
    }

    public static KeyPair generate(int bits, Random random) {
        if (bits < 512) {
            throw new IllegalArgumentException(
                    "an rsa key of " + bits + " bits is too small to be worth making");
        }
        BigInteger p;
        BigInteger q;
        BigInteger modulus;
        BigInteger totient;
        do {
            p = prime(bits / 2, random);
            q = prime(bits - bits / 2, random);
            modulus = p.multiply(q);
            totient = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        } while (p.equals(q) || modulus.bitLength() != bits
                || !totient.gcd(EXPONENT).equals(BigInteger.ONE));

        BigInteger privateExponent = EXPONENT.modInverse(totient);
        return new KeyPair(new Public(modulus, EXPONENT), new Private(modulus, privateExponent));
    }

    private static BigInteger prime(int bits, Random random) {
        while (true) {
            BigInteger candidate = new BigInteger(bits, 40, random);
            if (candidate.bitLength() == bits) {
                return candidate;
            }
        }
    }

    public static BigInteger apply(BigInteger message, BigInteger exponent, BigInteger modulus) {
        if (message.signum() < 0 || message.compareTo(modulus) >= 0) {
            throw new IllegalArgumentException(
                    "an rsa message has to be smaller than the key it goes through");
        }
        return message.modPow(exponent, modulus);
    }

    public static final class Public implements PublicKey {

        private final BigInteger modulus;
        private final BigInteger exponent;

        Public(BigInteger modulus, BigInteger exponent) {
            this.modulus = modulus;
            this.exponent = exponent;
        }

        public BigInteger modulus() {
            return modulus;
        }

        public BigInteger exponent() {
            return exponent;
        }

        @Override
        public String getAlgorithm() {
            return "RSA";
        }

        @Override
        public String getFormat() {
            return "X.509";
        }

        @Override
        public byte[] getEncoded() {
            byte[] numbers = Der.write(Der.SEQUENCE,
                    Der.join(Der.writeInteger(modulus), Der.writeInteger(exponent)));
            return Der.write(Der.SEQUENCE,
                    Der.join(Der.RSA_ALGORITHM, Der.writeBitString(numbers)));
        }
    }

    public static final class Private implements PrivateKey {

        private final BigInteger modulus;
        private final BigInteger exponent;

        Private(BigInteger modulus, BigInteger exponent) {
            this.modulus = modulus;
            this.exponent = exponent;
        }

        public BigInteger modulus() {
            return modulus;
        }

        public BigInteger exponent() {
            return exponent;
        }

        @Override
        public String getAlgorithm() {
            return "RSA";
        }

        @Override
        public String getFormat() {
            return "PKCS#8";
        }

        @Override
        public byte[] getEncoded() {
            byte[] numbers = Der.write(Der.SEQUENCE, Der.join(
                    Der.writeInteger(BigInteger.ZERO),
                    Der.writeInteger(modulus),
                    Der.writeInteger(EXPONENT),
                    Der.writeInteger(exponent),
                    Der.writeInteger(BigInteger.ZERO),
                    Der.writeInteger(BigInteger.ZERO),
                    Der.writeInteger(BigInteger.ZERO),
                    Der.writeInteger(BigInteger.ZERO),
                    Der.writeInteger(BigInteger.ZERO)));
            return Der.write(Der.SEQUENCE, Der.join(
                    Der.writeInteger(BigInteger.ZERO),
                    Der.RSA_ALGORITHM,
                    Der.write(Der.OCTET_STRING, numbers)));
        }
    }

    public static Public readPublic(byte[] encoded) {
        byte[] body = Der.read(encoded, 0).body();
        java.util.List<Der.Piece> outer = Der.readAll(body);
        byte[] numbers = null;
        for (Der.Piece piece : outer) {
            if (piece.tag() == Der.BIT_STRING) {
                numbers = Der.bitStringBody(piece.body());
            }
        }
        if (numbers == null) {
            throw new IllegalArgumentException(
                    "these X.509 bytes carry no key: there is no bit string in them");
        }
        java.util.List<Der.Piece> pair = Der.readAll(Der.read(numbers, 0).body());
        if (pair.size() < 2) {
            throw new IllegalArgumentException("an rsa key is two numbers, and these are not");
        }
        return new Public(new BigInteger(pair.get(0).body()),
                new BigInteger(pair.get(1).body()));
    }

    public static Private readPrivate(byte[] encoded) {
        java.util.List<Der.Piece> outer = Der.readAll(Der.read(encoded, 0).body());
        byte[] wrapped = null;
        for (Der.Piece piece : outer) {
            if (piece.tag() == Der.OCTET_STRING) {
                wrapped = piece.body();
            }
        }
        if (wrapped == null) {
            throw new IllegalArgumentException(
                    "these PKCS#8 bytes carry no key: there is no octet string in them");
        }
        java.util.List<Der.Piece> numbers = Der.readAll(Der.read(wrapped, 0).body());
        if (numbers.size() < 4) {
            throw new IllegalArgumentException("an rsa private key is written as nine numbers");
        }
        return new Private(new BigInteger(numbers.get(1).body()),
                new BigInteger(numbers.get(3).body()));
    }
}
