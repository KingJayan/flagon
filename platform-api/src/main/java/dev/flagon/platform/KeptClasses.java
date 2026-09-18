package dev.flagon.platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** lists the classes that flagon.preserve kept, so a classpath scan can find them */
public final class KeptClasses {

    static final String ROOT = "/flagon/classes";

    private static final List<String> LAID_OUT = new ArrayList<>();

    private KeptClasses() {
    }

    /** the build writes the kept names here, separated by commas */
    static String names() {
        return "";
    }

    /** tells if the package holds a kept class */
    static boolean holds(String path) {
        String prefix = path.replace('/', '.') + ".";
        for (String name : names().split(",")) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** writes the package out as empty files, and gives back the directory to read */
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
