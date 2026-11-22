package java.security;

import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** the nist prime curves, their keys, ecdsa and ecdh */
public final class Ec {

    private static final byte[] EC_KEY_ID = {
        0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x02, 0x01,
    };

    /** a named curve and the object id that names it in der */
    public record Curve(String name, int bits, byte[] id, ECParameterSpec params) {

        BigInteger p() {
            return ((ECFieldFp) params.getCurve().getField()).getP();
        }

        BigInteger a() {
            return params.getCurve().getA();
        }

        BigInteger n() {
            return params.getOrder();
        }

        int size() {
            return (bits + 7) / 8;
        }
    }

    private static final List<Curve> CURVES = List.of(
            curve("secp256r1", 256, new byte[] {0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03,
                0x01, 0x07},
                    "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff",
                    "5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b",
                    "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296",
                    "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5",
                    "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551"),
            curve("secp384r1", 384, new byte[] {0x2B, (byte) 0x81, 0x04, 0x00, 0x22},
                    "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffe"
                            + "ffffffff0000000000000000ffffffff",
                    "b3312fa7e23ee7e4988e056be3f82d19181d9c6efe8141120314088f5013875a"
                            + "c656398d8a2ed19d2a85c8edd3ec2aef",
                    "aa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a38"
                            + "5502f25dbf55296c3a545e3872760ab7",
                    "3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c0"
                            + "0a60b1ce1d7e819d7a431d7c90ea0e5f",
                    "ffffffffffffffffffffffffffffffffffffffffffffffffc7634d81f4372ddf"
                            + "581a0db248b0a77aecec196accc52973"),
            curve("secp521r1", 521, new byte[] {0x2B, (byte) 0x81, 0x04, 0x00, 0x23},
                    "1ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                            + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                    "51953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109"
                            + "e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00",
                    "c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3d"
                            + "baa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66",
                    "11839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e66"
                            + "2c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650",
                    "1ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                            + "fa51868783bf2f966b7fcc0148f709a5d03bb5c9b8899c47aebb6fb7"
                            + "1e91386409"));

    private Ec() {
    }

    private static Curve curve(String name, int bits, byte[] id, String p, String b, String x,
                               String y, String n) {
        BigInteger prime = new BigInteger(p, 16);
        EllipticCurve shape = new EllipticCurve(new ECFieldFp(prime),
                prime.subtract(BigInteger.valueOf(3)), new BigInteger(b, 16));
        return new Curve(name, bits, id, new ECParameterSpec(shape,
                new ECPoint(new BigInteger(x, 16), new BigInteger(y, 16)),
                new BigInteger(n, 16), 1));
    }

    /** @return the curve by any of its names, or null */
    public static Curve named(String name) {
        String asked = name.toLowerCase(Locale.ROOT).replace(" ", "").replace("nist", "");
        for (Curve curve : CURVES) {
            String digits = String.valueOf(curve.bits());
            if (asked.equals(curve.name()) || asked.equals("p-" + digits)
                    || asked.equals("secp" + digits + "r1")
                    || curve.bits() == 256 && asked.equals("prime256v1")) {
                return curve;
            }
        }
        return null;
    }

    /** @return the curve of this size, or null */
    public static Curve sized(int bits) {
        for (Curve curve : CURVES) {
            if (curve.bits() == bits) {
                return curve;
            }
        }
        return null;
    }

    /** @return the named curve these parameters describe, or null */
    public static Curve of(ECParameterSpec params) {
        for (Curve curve : CURVES) {
            ECParameterSpec known = curve.params();
            if (known == params || known.getCurve().equals(params.getCurve())
                    && known.getGenerator().equals(params.getGenerator())
                    && known.getOrder().equals(params.getOrder())
                    && known.getCofactor() == params.getCofactor()) {
                return curve;
            }
        }
        return null;
    }

    private static Curve byId(byte[] id) {
        for (Curve curve : CURVES) {
            if (Arrays.equals(curve.id(), id)) {
                return curve;
            }
        }
        throw new IllegalArgumentException("these bytes name a curve that is not written here;"
                + " secp256r1, secp384r1 and secp521r1 are");
    }

    public static KeyPair generate(Curve curve, Random random) {
        BigInteger s = scalar(curve, random);
        return new KeyPair(new Public(curve, multiply(curve, s, curve.params().getGenerator())),
                new Private(curve, s));
    }

    /** a secret in [1, n - 1] */
    static BigInteger scalar(Curve curve, Random random) {
        BigInteger n = curve.n();
        while (true) {
            BigInteger candidate = new BigInteger(n.bitLength(), random);
            if (candidate.signum() > 0 && candidate.compareTo(n) < 0) {
                return candidate;
            }
        }
    }

    public static boolean isOnCurve(Curve curve, ECPoint point) {
        if (point == ECPoint.POINT_INFINITY) {
            return false;
        }
        BigInteger p = curve.p();
        BigInteger x = point.getAffineX();
        BigInteger y = point.getAffineY();
        if (x.signum() < 0 || x.compareTo(p) >= 0 || y.signum() < 0 || y.compareTo(p) >= 0) {
            return false;
        }
        BigInteger right = x.pow(3).add(curve.a().multiply(x))
                .add(curve.params().getCurve().getB()).mod(p);
        return y.pow(2).mod(p).equals(right);
    }

    /** the shared x coordinate, as many bytes as the field is wide */
    public static byte[] agree(Private mine, ECPublicKey theirs) {
        Curve curve = mine.curve();
        ECPoint shared = multiply(curve, mine.getS(), theirs.getW());
        if (shared == ECPoint.POINT_INFINITY) {
            throw new IllegalStateException("the shared point is the point at infinity");
        }
        return fixed(shared.getAffineX(), curve.size());
    }

    /** an ecdsa signature over a digest, as the der pair of r and s */
    public static byte[] sign(Private key, byte[] digest, Random random) {
        Curve curve = key.curve();
        BigInteger n = curve.n();
        BigInteger z = truncated(digest, n);
        while (true) {
            BigInteger k = scalar(curve, random);
            BigInteger r = multiply(curve, k, curve.params().getGenerator()).getAffineX().mod(n);
            if (r.signum() == 0) {
                continue;
            }
            BigInteger s = k.modInverse(n).multiply(z.add(r.multiply(key.getS()))).mod(n);
            if (s.signum() != 0) {
                return Der.write(Der.SEQUENCE, Der.join(Der.writeInteger(r), Der.writeInteger(s)));
            }
        }
    }

    /** @throws IllegalArgumentException when the signature is not a der pair */
    public static boolean verify(Public key, byte[] digest, byte[] signature) {
        Curve curve = key.curve();
        BigInteger n = curve.n();
        Der.Piece pair = Der.read(signature, 0);
        List<Der.Piece> numbers = Der.readAll(pair.body());
        if (pair.tag() != Der.SEQUENCE || pair.after() != signature.length
                || numbers.size() != 2 || numbers.get(0).tag() != Der.INTEGER
                || numbers.get(1).tag() != Der.INTEGER) {
            throw new IllegalArgumentException("not a der pair of integers");
        }
        BigInteger r = new BigInteger(numbers.get(0).body());
        BigInteger s = new BigInteger(numbers.get(1).body());
        if (r.signum() <= 0 || r.compareTo(n) >= 0 || s.signum() <= 0 || s.compareTo(n) >= 0) {
            return false;
        }
        BigInteger w = s.modInverse(n);
        BigInteger u1 = truncated(digest, n).multiply(w).mod(n);
        BigInteger u2 = r.multiply(w).mod(n);
        ECPoint at = add(curve, multiply(curve, u1, curve.params().getGenerator()),
                multiply(curve, u2, key.getW()));
        return at != ECPoint.POINT_INFINITY && at.getAffineX().mod(n).equals(r);
    }

    /** the leftmost bits of a digest, as many as the order has */
    private static BigInteger truncated(byte[] digest, BigInteger n) {
        BigInteger z = new BigInteger(1, digest);
        int extra = digest.length * 8 - n.bitLength();
        return extra > 0 ? z.shiftRight(extra) : z;
    }

    // points in jacobian coordinates: x = X / Z^2, y = Y / Z^3, and Z = 0 at infinity

    static ECPoint multiply(Curve curve, BigInteger k, ECPoint point) {
        BigInteger p = curve.p();
        BigInteger[] base = {point.getAffineX(), point.getAffineY(), BigInteger.ONE};
        BigInteger[] sum = {BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO};
        BigInteger scalar = k.mod(curve.n());
        for (int bit = scalar.bitLength() - 1; bit >= 0; bit--) {
            sum = twice(sum, curve.a(), p);
            if (scalar.testBit(bit)) {
                sum = plus(sum, base, curve.a(), p);
            }
        }
        return affine(sum, p);
    }

    private static ECPoint add(Curve curve, ECPoint left, ECPoint right) {
        if (left == ECPoint.POINT_INFINITY) {
            return right;
        }
        if (right == ECPoint.POINT_INFINITY) {
            return left;
        }
        BigInteger p = curve.p();
        return affine(plus(new BigInteger[] {left.getAffineX(), left.getAffineY(), BigInteger.ONE},
                new BigInteger[] {right.getAffineX(), right.getAffineY(), BigInteger.ONE},
                curve.a(), p), p);
    }

    private static ECPoint affine(BigInteger[] point, BigInteger p) {
        if (point[2].signum() == 0) {
            return ECPoint.POINT_INFINITY;
        }
        BigInteger inverse = point[2].modInverse(p);
        BigInteger squared = inverse.multiply(inverse).mod(p);
        return new ECPoint(point[0].multiply(squared).mod(p),
                point[1].multiply(squared).multiply(inverse).mod(p));
    }

    private static BigInteger[] twice(BigInteger[] point, BigInteger a, BigInteger p) {
        if (point[2].signum() == 0 || point[1].signum() == 0) {
            return new BigInteger[] {BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO};
        }
        BigInteger x = point[0];
        BigInteger y = point[1];
        BigInteger z = point[2];
        BigInteger ySquared = y.multiply(y).mod(p);
        BigInteger s = x.multiply(ySquared).shiftLeft(2).mod(p);
        BigInteger zSquared = z.multiply(z).mod(p);
        BigInteger m = x.multiply(x).multiply(BigInteger.valueOf(3))
                .add(a.multiply(zSquared.multiply(zSquared))).mod(p);
        BigInteger nextX = m.multiply(m).subtract(s.shiftLeft(1)).mod(p);
        BigInteger nextY = m.multiply(s.subtract(nextX))
                .subtract(ySquared.multiply(ySquared).shiftLeft(3)).mod(p);
        BigInteger nextZ = y.multiply(z).shiftLeft(1).mod(p);
        return new BigInteger[] {nextX, nextY, nextZ};
    }

    private static BigInteger[] plus(BigInteger[] left, BigInteger[] right, BigInteger a,
                                     BigInteger p) {
        if (left[2].signum() == 0) {
            return right;
        }
        if (right[2].signum() == 0) {
            return left;
        }
        BigInteger leftZ2 = left[2].multiply(left[2]).mod(p);
        BigInteger rightZ2 = right[2].multiply(right[2]).mod(p);
        BigInteger u1 = left[0].multiply(rightZ2).mod(p);
        BigInteger u2 = right[0].multiply(leftZ2).mod(p);
        BigInteger s1 = left[1].multiply(rightZ2).multiply(right[2]).mod(p);
        BigInteger s2 = right[1].multiply(leftZ2).multiply(left[2]).mod(p);
        if (u1.equals(u2)) {
            return s1.equals(s2) ? twice(left, a, p)
                    : new BigInteger[] {BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO};
        }
        BigInteger h = u2.subtract(u1).mod(p);
        BigInteger r = s2.subtract(s1).mod(p);
        BigInteger h2 = h.multiply(h).mod(p);
        BigInteger h3 = h2.multiply(h).mod(p);
        BigInteger u1h2 = u1.multiply(h2).mod(p);
        BigInteger nextX = r.multiply(r).subtract(h3).subtract(u1h2.shiftLeft(1)).mod(p);
        BigInteger nextY = r.multiply(u1h2.subtract(nextX)).subtract(s1.multiply(h3)).mod(p);
        BigInteger nextZ = h.multiply(left[2]).multiply(right[2]).mod(p);
        return new BigInteger[] {nextX, nextY, nextZ};
    }

    static byte[] fixed(BigInteger value, int size) {
        byte[] bytes = value.toByteArray();
        byte[] out = new byte[size];
        if (bytes.length >= size) {
            System.arraycopy(bytes, bytes.length - size, out, 0, size);
        } else {
            System.arraycopy(bytes, 0, out, size - bytes.length, bytes.length);
        }
        return out;
    }

    private static byte[] algorithm(Curve curve) {
        return Der.write(Der.SEQUENCE, Der.join(Der.write(Der.OBJECT_ID, EC_KEY_ID),
                Der.write(Der.OBJECT_ID, curve.id())));
    }

    public static final class Public implements ECPublicKey {

        private final Curve curve;
        private final ECPoint w;

        public Public(Curve curve, ECPoint w) {
            if (!isOnCurve(curve, w)) {
                throw new IllegalArgumentException("the point is not on " + curve.name());
            }
            this.curve = curve;
            this.w = w;
        }

        Curve curve() {
            return curve;
        }

        @Override
        public ECPoint getW() {
            return w;
        }

        @Override
        public ECParameterSpec getParams() {
            return curve.params();
        }

        @Override
        public String getAlgorithm() {
            return "EC";
        }

        @Override
        public String getFormat() {
            return "X.509";
        }

        @Override
        public byte[] getEncoded() {
            byte[] point = Der.join(new byte[] {4}, fixed(w.getAffineX(), curve.size()),
                    fixed(w.getAffineY(), curve.size()));
            return Der.write(Der.SEQUENCE, Der.join(algorithm(curve), Der.writeBitString(point)));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ECPublicKey that && Arrays.equals(getEncoded(),
                    that.getEncoded());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(getEncoded());
        }

        @Override
        public String toString() {
            return "Sun EC public key, " + curve.bits() + " bits\n  public x coord: "
                    + w.getAffineX() + "\n  public y coord: " + w.getAffineY()
                    + "\n  parameters: " + curve.name();
        }
    }

    public static final class Private implements ECPrivateKey {

        private final Curve curve;
        private final BigInteger s;

        public Private(Curve curve, BigInteger s) {
            this.curve = curve;
            this.s = s;
        }

        Curve curve() {
            return curve;
        }

        @Override
        public BigInteger getS() {
            return s;
        }

        @Override
        public ECParameterSpec getParams() {
            return curve.params();
        }

        @Override
        public String getAlgorithm() {
            return "EC";
        }

        @Override
        public String getFormat() {
            return "PKCS#8";
        }

        /** written the way the jdk writes it: no curve or public point inside */
        @Override
        public byte[] getEncoded() {
            byte[] inner = Der.write(Der.SEQUENCE, Der.join(Der.writeInteger(BigInteger.ONE),
                    Der.write(Der.OCTET_STRING, fixed(s, curve.size()))));
            return Der.write(Der.SEQUENCE, Der.join(Der.writeInteger(BigInteger.ZERO),
                    algorithm(curve), Der.write(Der.OCTET_STRING, inner)));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ECPrivateKey that && Arrays.equals(getEncoded(),
                    that.getEncoded());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(getEncoded());
        }
    }

    public static Public readPublic(byte[] encoded) {
        List<Der.Piece> outer = Der.readAll(Der.read(encoded, 0).body());
        if (outer.size() != 2 || outer.get(1).tag() != Der.BIT_STRING) {
            throw new IllegalArgumentException("these X.509 bytes are not an ec public key");
        }
        Curve curve = curveIn(outer.get(0));
        byte[] point = Der.bitStringBody(outer.get(1).body());
        int size = curve.size();
        if (point.length != 1 + 2 * size || point[0] != 4) {
            throw new IllegalArgumentException("only uncompressed ec points are read here");
        }
        return new Public(curve, new ECPoint(
                new BigInteger(1, Arrays.copyOfRange(point, 1, 1 + size)),
                new BigInteger(1, Arrays.copyOfRange(point, 1 + size, point.length))));
    }

    public static Private readPrivate(byte[] encoded) {
        List<Der.Piece> outer = Der.readAll(Der.read(encoded, 0).body());
        if (outer.size() < 3 || outer.get(2).tag() != Der.OCTET_STRING) {
            throw new IllegalArgumentException("these PKCS#8 bytes are not an ec private key");
        }
        Curve curve = curveIn(outer.get(1));
        List<Der.Piece> inner = Der.readAll(Der.read(outer.get(2).body(), 0).body());
        if (inner.size() < 2 || inner.get(1).tag() != Der.OCTET_STRING) {
            throw new IllegalArgumentException("an ec private key holds its secret as octets");
        }
        return new Private(curve, new BigInteger(1, inner.get(1).body()));
    }

    private static Curve curveIn(Der.Piece algorithm) {
        List<Der.Piece> ids = Der.readAll(algorithm.body());
        if (ids.size() != 2 || !Arrays.equals(ids.get(0).body(), EC_KEY_ID)) {
            throw new IllegalArgumentException("these bytes do not hold an ec key");
        }
        return byId(ids.get(1).body());
    }
}
