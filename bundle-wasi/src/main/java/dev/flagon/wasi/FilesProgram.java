package dev.flagon.wasi;

import dev.flagon.runtime.Program;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.BufferedReader;
import java.util.Arrays;

/** java.io on the directory the host preopened as / */
public final class FilesProgram implements Program {

    @Override
    public String id() {
        return "files";
    }

    @Override
    public String description() {
        return "writes, reads, lists, moves and deletes files in the preopened directory";
    }

    @Override
    public void run(String[] arguments) throws IOException {
        File notes = new File("/notes/today.txt");
        System.out.println("made the directory: " + notes.getParentFile().mkdirs());
        try (FileWriter writer = new FileWriter(notes)) {
            writer.write("first line\nsecond line\n");
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(notes))) {
            System.out.println("read back: " + reader.readLine() + " / " + reader.readLine());
        }
        System.out.println("its length: " + notes.length());

        try (RandomAccessFile file = new RandomAccessFile("/notes/today.txt", "rw")) {
            file.seek(6);
            file.write("LINE".getBytes());
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(notes))) {
            System.out.println("patched: " + reader.readLine());
        }

        File moved = new File("/notes/kept.txt");
        System.out.println("moved: " + notes.renameTo(moved) + " " + notes.exists() + " "
                + moved.exists());
        new File("/notes/scratch.txt").createNewFile();
        String[] names = new File("/notes").list();
        Arrays.sort(names);
        System.out.println("listed: " + String.join(", ", names));
        System.out.println("deleted: " + new File("/notes/scratch.txt").delete() + " "
                + new File("/notes/missing.txt").delete() + " " + new File("/notes").delete());
    }
}
