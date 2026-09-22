package dev.flagon.platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class KeptClasses {

    static final String ROOT = "/flagon/classes";

    private static final Set<String> LAID_OUT = new HashSet<>();

    private static String[] sorted;

    private KeptClasses() {
    }

    // the build writes the kept names here
    static String names() {
        return "";
    }

    // the list runs to tens of thousands of names, so it is split and sorted once
    private static String[] sorted() {
        if (sorted == null) {
            String all = names();
            sorted = all.isEmpty() ? new String[0] : all.split(",");
            Arrays.sort(sorted);
        }
        return sorted;
    }

    /** @return where the names under a package start */
    private static int from(String prefix) {
        String[] names = sorted();
        int low = 0;
        int high = names.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (names[middle].compareTo(prefix) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    static boolean holds(String path) {
        String prefix = path.replace('/', '.') + ".";
        String[] names = sorted();
        int at = from(prefix);
        return at < names.length && names[at].startsWith(prefix);
    }

    // a scan reads a directory, so write one
    static File layOut(String path) {
        File directory = new File(ROOT + "/" + path);
        if (!LAID_OUT.add(path)) {
            return directory;
        }
        String prefix = path.replace('/', '.') + ".";
        String[] names = sorted();
        for (int at = from(prefix); at < names.length && names[at].startsWith(prefix); at++) {
            write(new File(ROOT + "/" + names[at].replace('.', '/') + ".class"));
        }
        return directory;
    }

    private static void write(File file) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (FileOutputStream empty = new FileOutputStream(file)) {
            empty.flush();
        } catch (IOException cannotWrite) {
            // the scan does not see this class
        }
    }
}
