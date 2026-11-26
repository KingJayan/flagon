package java.security;

import java.math.BigInteger;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

final class Der {

    static final int SEQUENCE = 0x30;
    static final int INTEGER = 0x02;
    static final int BIT_STRING = 0x03;
    static final int OCTET_STRING = 0x04;
    static final int NULL = 0x05;
    static final int OBJECT_ID = 0x06;

    static final byte[] RSA_ALGORITHM = {
        0x30, 0x0D, 0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D,
        0x01, 0x01, 0x01, 0x05, 0x00,
    };

    private Der() {
    }

    record Piece(int tag, byte[] body, int after) {
    }

    static Piece read(byte[] bytes, int at) {
        if (at + 2 > bytes.length) {
            throw new IllegalArgumentException("a der value needs a tag and a length");
        }
        int tag = bytes[at] & 0xFF;
        int lengthByte = bytes[at + 1] & 0xFF;
        int length;
        int bodyAt;
        if (lengthByte < 0x80) {
            length = lengthByte;
            bodyAt = at + 2;
        } else {
            int count = lengthByte & 0x7F;
            if (count == 0 || count > 4) {
                throw new IllegalArgumentException(
                        "a der length of " + count + " bytes is not read here");
            }
            length = 0;
            for (int step = 0; step < count; step++) {
                length = length << 8 | (bytes[at + 2 + step] & 0xFF);
            }
            bodyAt = at + 2 + count;
        }
        if (length < 0 || bodyAt + length > bytes.length) {
            throw new IllegalArgumentException("a der value says it is longer than what is left");
        }
        byte[] body = new byte[length];
        System.arraycopy(bytes, bodyAt, body, 0, length);
        return new Piece(tag, body, bodyAt + length);
    }

    static List<Piece> readAll(byte[] bytes) {
        List<Piece> pieces = new ArrayList<>();
        int at = 0;
        while (at < bytes.length) {
            Piece piece = read(bytes, at);
            pieces.add(piece);
            at = piece.after();
        }
        return pieces;
    }

    static byte[] write(int tag, byte[] body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        if (body.length < 0x80) {
            out.write(body.length);
        } else {
            int count = body.length > 0xFFFF ? 3 : body.length > 0xFF ? 2 : 1;
            out.write(0x80 | count);
            for (int step = count - 1; step >= 0; step--) {
                out.write(body.length >>> (step * 8));
            }
        }
        out.write(body, 0, body.length);
        return out.toByteArray();
    }

    static byte[] writeInteger(BigInteger value) {
        return write(INTEGER, value.toByteArray());
    }

    static byte[] join(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    static byte[] writeBitString(byte[] body) {
        return write(BIT_STRING, join(new byte[] {0}, body));
    }

    static byte[] bitStringBody(byte[] body) {
        if (body.length == 0) {
            throw new IllegalArgumentException("a bit string carries no unused-bit count");
        }
        byte[] out = new byte[body.length - 1];
        System.arraycopy(body, 1, out, 0, out.length);
        return out;
    }
}
