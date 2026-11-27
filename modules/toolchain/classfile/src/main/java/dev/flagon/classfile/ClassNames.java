package dev.flagon.classfile;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * the ways one class file can reach a {@link Class} it has no instance of: class literals in its
 * code, strings that name a class, and the types of its fields and generic signatures, which
 * reflective readers such as gson construct. names use dots.
 */
public record ClassNames(String name, Set<String> literals, Set<String> strings,
        Set<String> fieldTypes, Set<String> signatureTypes) {
    // signatureTypes: the type arguments of the class's own generic signature, as a gson
    // TypeToken subclass names the type it reads

    private static final int UTF8 = 1;
    private static final int CLASS = 7;
    private static final int STRING = 8;
    private static final int LDC = 0x12;
    private static final int LDC_W = 0x13;

    private static final Pattern SIGNATURE_TYPE = Pattern.compile("L([^;<]+)");

    public static ClassNames of(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != 0xCAFEBABE) {
            return null;
        }
        in.skipBytes(4);
        int count = in.readUnsignedShort();
        int[] tags = new int[count];
        String[] text = new String[count];
        int[] first = new int[count];
        for (int at = 1; at < count; at++) {
            int tag = in.readUnsignedByte();
            tags[at] = tag;
            switch (tag) {
                case UTF8 -> text[at] = in.readUTF();
                case CLASS, STRING, 16, 19, 20 -> first[at] = in.readUnsignedShort();
                case 15 -> in.skipBytes(3);
                case 5, 6 -> {
                    in.skipBytes(8);
                    at++;
                }
                default -> in.skipBytes(4);
            }
        }
        Set<String> strings = new LinkedHashSet<>();
        for (int at = 1; at < count; at++) {
            if (tags[at] == STRING) {
                strings.add(text[first[at]].replace('/', '.'));
            }
        }

        in.skipBytes(2);
        String name = dotted(text[first[in.readUnsignedShort()]]);
        in.skipBytes(2);
        in.skipBytes(2 * in.readUnsignedShort());

        Set<String> literals = new LinkedHashSet<>();
        Set<String> fieldTypes = new LinkedHashSet<>();
        Set<String> signatureTypes = new LinkedHashSet<>();
        int fields = in.readUnsignedShort();
        for (int field = 0; field < fields; field++) {
            in.skipBytes(4);
            typesIn(text[in.readUnsignedShort()], fieldTypes);
            readAttributes(in, text, attribute -> {
                if (attribute.named("Signature")) {
                    typesIn(text[attribute.in().readUnsignedShort()], fieldTypes);
                }
            });
        }
        int methods = in.readUnsignedShort();
        for (int method = 0; method < methods; method++) {
            in.skipBytes(6);
            readAttributes(in, text, attribute -> {
                if (attribute.named("Code")) {
                    literalsIn(attribute.in(), tags, text, first, literals);
                }
            });
        }
        readAttributes(in, text, attribute -> {
            if (attribute.named("Signature")) {
                typesIn(typeArguments(text[attribute.in().readUnsignedShort()]), signatureTypes);
            }
        });
        return new ClassNames(name, literals, strings, fieldTypes, signatureTypes);
    }

    private record Attribute(String name, DataInputStream in) {
        boolean named(String wanted) {
            return wanted.equals(name);
        }
    }

    private interface AttributeReader {
        void read(Attribute attribute) throws IOException;
    }

    private static void readAttributes(DataInputStream in, String[] text, AttributeReader reader)
            throws IOException {
        int attributes = in.readUnsignedShort();
        for (int at = 0; at < attributes; at++) {
            String name = text[in.readUnsignedShort()];
            byte[] body = in.readNBytes(in.readInt());
            reader.read(new Attribute(name, new DataInputStream(new ByteArrayInputStream(body))));
        }
    }

    private static void literalsIn(DataInputStream in, int[] tags, String[] text, int[] first,
                                   Set<String> into) throws IOException {
        in.skipBytes(4);
        byte[] code = in.readNBytes(in.readInt());
        int at = 0;
        while (at < code.length) {
            int op = code[at] & 0xff;
            int index = op == LDC ? code[at + 1] & 0xff
                    : op == LDC_W ? (code[at + 1] & 0xff) << 8 | code[at + 2] & 0xff : 0;
            if (index > 0 && tags[index] == CLASS) {
                String literal = text[first[index]];
                if (literal.startsWith("[")) {
                    typesIn(literal, into);
                } else {
                    into.add(dotted(literal));
                }
            }
            at += lengthAt(code, at);
        }
    }

    /** the length of the instruction at {@code at}, operands included */
    static int lengthAt(byte[] code, int at) {
        int op = code[at] & 0xff;
        switch (op) {
            case 0xaa -> {
                int pad = (at + 4) & ~3;
                return pad + 12 + 4 * (intAt(code, pad + 8) - intAt(code, pad + 4) + 1) - at;
            }
            case 0xab -> {
                int pad = (at + 4) & ~3;
                return pad + 8 + 8 * intAt(code, pad + 4) - at;
            }
            case 0xc4 -> {
                return (code[at + 1] & 0xff) == 0x84 ? 6 : 4;
            }
            case 0x10, 0x12, 0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3a, 0xa9,
                 0xbc -> {
                return 2;
            }
            case 0x11, 0x13, 0x14, 0x84, 0xbb, 0xbd, 0xc0, 0xc1, 0xc6, 0xc7 -> {
                return 3;
            }
            case 0xc5 -> {
                return 4;
            }
            case 0xb9, 0xba, 0xc8, 0xc9 -> {
                return 5;
            }
            default -> {
                return op >= 0x99 && op <= 0xa8 || op >= 0xb2 && op <= 0xb8 ? 3 : 1;
            }
        }
    }

    private static int intAt(byte[] code, int at) {
        return (code[at] & 0xff) << 24 | (code[at + 1] & 0xff) << 16
                | (code[at + 2] & 0xff) << 8 | code[at + 3] & 0xff;
    }

    /** what a signature holds inside angle brackets, so the type arguments and bounds */
    private static String typeArguments(String signature) {
        StringBuilder inside = new StringBuilder();
        int depth = 0;
        for (char next : signature.toCharArray()) {
            if (next == '>') {
                depth--;
            }
            if (depth > 0) {
                inside.append(next);
            }
            if (next == '<') {
                depth++;
                inside.append(' ');
            }
        }
        return inside.toString();
    }

    /** the classes a descriptor or signature names; a type variable may add a stray name */
    private static void typesIn(String raw, Set<String> into) {
        Matcher found = SIGNATURE_TYPE.matcher(raw);
        while (found.find()) {
            into.add(dotted(found.group(1)));
        }
    }

    private static String dotted(String raw) {
        return raw.replace('/', '.');
    }
}
