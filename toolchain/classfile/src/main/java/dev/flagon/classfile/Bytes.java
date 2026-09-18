package dev.flagon.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record Bytes(Set<String> classes, List<String[]> calls, Set<String> members,
        String[] parents) {

    private static final int UTF8 = 1;
    private static final int CLASS = 7;
    private static final int METHOD = 10;
    private static final int INTERFACE_METHOD = 11;
    private static final int NAME_AND_TYPE = 12;

    public static Bytes of(DataInputStream in) throws IOException {
        if (in.readInt() != 0xCAFEBABE) {
            return null;
        }
        in.skipBytes(4);

        int count = in.readUnsignedShort();
        int[] tags = new int[count];
        String[] text = new String[count];
        int[] first = new int[count];
        int[] second = new int[count];
        for (int at = 1; at < count; at++) {
            int tag = in.readUnsignedByte();
            tags[at] = tag;
            switch (tag) {
                case UTF8 -> text[at] = in.readUTF();
                case CLASS, 8, 16, 19, 20 -> first[at] = in.readUnsignedShort();
                case 15 -> in.skipBytes(3);
                case 5, 6 -> {
                    in.skipBytes(8);
                    at++;
                }
                default -> {
                    first[at] = in.readUnsignedShort();
                    second[at] = in.readUnsignedShort();
                }
            }
        }

        Set<String> classes = new LinkedHashSet<>();
        List<String[]> calls = new ArrayList<>();
        for (int at = 1; at < count; at++) {
            if (tags[at] == CLASS) {
                named(text[first[at]], classes);
            } else if (tags[at] == METHOD || tags[at] == INTERFACE_METHOD) {
                int nameAndType = second[at];
                if (tags[nameAndType] != NAME_AND_TYPE) {
                    continue;
                }
                String owner = dotted(text[first[first[at]]]);
                calls.add(new String[] {owner,
                    text[first[nameAndType]] + text[second[nameAndType]]});
            }
        }

        in.skipBytes(4);
        String parent = text[first[in.readUnsignedShort()]];
        List<String> parents = new ArrayList<>();
        if (parent != null) {
            parents.add(dotted(parent));
        }
        int interfaces = in.readUnsignedShort();
        for (int at = 0; at < interfaces; at++) {
            parents.add(dotted(text[first[in.readUnsignedShort()]]));
        }

        skipMembers(in);
        Set<String> members = new LinkedHashSet<>();
        int methods = in.readUnsignedShort();
        for (int at = 0; at < methods; at++) {
            in.skipBytes(2);
            members.add(text[in.readUnsignedShort()] + text[in.readUnsignedShort()]);
            skipAttributes(in);
        }
        return new Bytes(classes, calls, members, parents.toArray(new String[0]));
    }

    /** reads the item type of an array, and skips a primitive array */
    private static void named(String raw, Set<String> into) {
        String name = raw;
        while (name.startsWith("[")) {
            name = name.substring(1);
        }
        if (name.startsWith("L") && name.endsWith(";")) {
            name = name.substring(1, name.length() - 1);
        } else if (name.length() <= 1) {
            return;
        }
        into.add(dotted(name));
    }

    private static String dotted(String raw) {
        return raw.replace('/', '.');
    }

    private static void skipMembers(DataInputStream in) throws IOException {
        int members = in.readUnsignedShort();
        for (int at = 0; at < members; at++) {
            in.skipBytes(6);
            skipAttributes(in);
        }
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
        int attributes = in.readUnsignedShort();
        for (int at = 0; at < attributes; at++) {
            in.skipBytes(2);
            in.skipBytes(in.readInt());
        }
    }
}
