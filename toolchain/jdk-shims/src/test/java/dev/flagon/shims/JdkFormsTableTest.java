package dev.flagon.shims;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * the browser classlib writes jdk classes from a table of their serial forms; this checks each
 * row against the jdk this test runs on.
 */
class JdkFormsTableTest {

    private static final Path FORMS = Path.of(
            "../jdk-classlib/src/main/java/java/io/JdkForms.java");

    @Test
    void eachFormIsTheJdksOwn() throws IOException, ClassNotFoundException {
        String source = Files.readString(FORMS);
        int start = source.indexOf("TABLE = \"\"\"") + "TABLE = \"\"\"".length();
        // a row ending in a backslash continues on the next line, as in a text block
        String table = source.substring(start, source.indexOf("\"\"\";", start))
                .replaceAll("\\\\\n", "");
        List<String> wrong = new ArrayList<>();
        int rows = 0;
        for (String line : table.split("\n")) {
            if (line.isBlank()) {
                continue;
            }
            rows++;
            String[] parts = line.trim().split("\\s+");
            Class<?> type = Class.forName(parts[0]);
            ObjectStreamClass jdk = ObjectStreamClass.lookup(type);
            expect(wrong, parts[0] + " serialVersionUID", jdk.getSerialVersionUID(),
                    Long.parseLong(parts[1]));
            int flags = Integer.parseInt(parts[2], 16);
            expect(wrong, parts[0] + " writeObject", writesItself(type), (flags & 0x01) != 0);
            Class<?> parent = type.getSuperclass();
            String superName = parent != null && Serializable.class.isAssignableFrom(parent)
                    ? parent.getName() : "-";
            expect(wrong, parts[0] + " superclass", superName, parts[3]);
            Set<String> fields = new TreeSet<>();
            for (ObjectStreamField field : jdk.getFields()) {
                fields.add((field.isPrimitive() ? String.valueOf(field.getTypeCode())
                        : field.getTypeString()) + ":" + field.getName());
            }
            Set<String> written = new TreeSet<>(List.of(parts).subList(4, parts.length));
            expect(wrong, parts[0] + " fields", fields, written);
        }
        assertTrue(rows > 30, "read " + rows + " rows");
        assertEquals(List.of(), wrong);
    }

    private static boolean writesItself(Class<?> type) {
        try {
            Method method = type.getDeclaredMethod("writeObject", ObjectOutputStream.class);
            return Modifier.isPrivate(method.getModifiers());
        } catch (NoSuchMethodException absent) {
            return false;
        }
    }

    private static void expect(List<String> wrong, String what, Object jdk, Object table) {
        if (!jdk.equals(table)) {
            wrong.add(what + ": the jdk says " + jdk + ", the table says " + table);
        }
    }
}
