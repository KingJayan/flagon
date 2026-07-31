package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 unmodified java.io against the browser's own storage, including across a reload
 */
public final class FileSystemDemoProgram implements Program {

    private static final String HOME = "/flagon";
    private static final String COUNTER = HOME + "/runs.txt";

    @Override
    public String id() {
        return "files";
    }

    @Override
    public String description() {
        return "reads and writes files through java.io";
    }

    @Override
    public void run(String[] arguments) throws Exception {
        ConsoleSink console = Platforms.get().console();
        console.info("file store: " + Platforms.get().files().name());

        File home = new File(HOME);
        if (!home.exists() && !home.mkdir()) {
            throw new IllegalStateException("could not create " + HOME);
        }

        console.info("run " + count());
        streams(console);
        randomAccess(console);
        directories(console);
        console.info("files: done");
    }

    /** the count only grows if the browser kept the file from the last page load. */
    private int count() throws Exception {
        File counter = new File(COUNTER);
        int previous = 0;
        if (counter.isFile()) {
            previous = Integer.parseInt(read(counter).trim());
        }
        int run = previous + 1;
        try (FileOutputStream out = new FileOutputStream(counter)) {
            out.write(Integer.toString(run).getBytes(StandardCharsets.UTF_8));
        }
        return run;
    }

    private void streams(ConsoleSink console) throws Exception {
        File notes = new File(HOME, "notes.txt");
        try (FileOutputStream out = new FileOutputStream(notes)) {
            out.write("hello from java.io".getBytes(StandardCharsets.UTF_8));
        }
        console.info("wrote " + notes.getPath() + " (" + notes.length() + " bytes)");
        console.info("read back: " + read(notes));
    }

    /** seek-and-overwrite, the access pattern a whole-file store has to keep honest. */
    private void randomAccess(ConsoleSink console) throws Exception {
        File notes = new File(HOME, "notes.txt");
        try (RandomAccessFile file = new RandomAccessFile(notes, "rw")) {
            file.seek(6);
            file.write("java.io says".getBytes(StandardCharsets.UTF_8));
        }
        console.info("after seek: " + read(notes));
    }

    private void directories(ConsoleSink console) throws Exception {
        File saves = new File(HOME, "saves");
        if (!saves.exists() && !saves.mkdir()) {
            throw new IllegalStateException("could not create " + saves.getPath());
        }
        File scratch = new File(HOME, "scratch.dat");
        try (FileOutputStream out = new FileOutputStream(scratch)) {
            out.write(new byte[] {1, 2, 3, 4});
        }

        File kept = new File(saves, "kept.dat");
        if (kept.exists() && !kept.delete()) {
            throw new IllegalStateException("could not clear " + kept.getPath());
        }
        console.info("renamed: " + scratch.renameTo(kept));

        String[] names = saves.list();
        Arrays.sort(names);
        console.info("saves holds " + Arrays.toString(names));
        console.info("scratch is gone: " + !scratch.exists());
        console.info("deleted kept.dat: " + kept.delete());
    }

    private static String read(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int read = in.read(data);
            return new String(data, 0, Math.max(read, 0), StandardCharsets.UTF_8);
        }
    }
}
