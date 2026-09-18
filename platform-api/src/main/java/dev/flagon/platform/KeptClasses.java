package dev.flagon.platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class KeptClasses {

    static final String ROOT = "/flagon/classes";

    private static final List<String> LAID_OUT = new ArrayList<>();

    private KeptClasses() {
    }

    // the build writes the kept names here
    static String names() {
        return "";
    }

    static boolean holds(String path) {
        String prefix = path.replace('/', '.') + ".";
        for (String name : names().split(",")) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // a scan reads a directory, so write one
    static File layOut(String path) {
        File directory = new File(ROOT + "/" + path);
        if (LAID_OUT.contains(path)) {
            return directory;
        }
        LAID_OUT.add(path);
        String prefix = path.replace('/', '.') + ".";
        for (String name : names().split(",")) {
            if (name.startsWith(prefix)) {
                write(new File(ROOT + "/" + name.replace('.', '/') + ".class"));
            }
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
