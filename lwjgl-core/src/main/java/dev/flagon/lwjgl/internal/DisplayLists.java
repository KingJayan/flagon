package dev.flagon.lwjgl.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * gl display lists as commands
 */
public final class DisplayLists {

    public static final int COMPILE = 0x1300;
    public static final int COMPILE_AND_EXECUTE = 0x1301;

    /** what one list name holds */
    private static final class Held {

        private final List<Runnable> commands;
        private List<Integer> blocks;

        Held(List<Runnable> commands, List<Integer> blocks) {
            this.commands = commands;
            this.blocks = blocks;
        }
    }

    // a list name is its index
    private final List<Held> lists = new ArrayList<>();
    private IntConsumer release = block -> { };
    private List<Runnable> recording;
    private List<Integer> recordingBlocks;
    private int recordingInto;
    private boolean executeWhileRecording;
    private int nextName = 1;

    /** a list owns the vertices its blocks uploaded, so they go when the list does */
    public void onRelease(IntConsumer release) {
        this.release = release;
    }

    public void own(int block) {
        if (recordingBlocks != null) {
            recordingBlocks.add(block);
        }
    }

    public int generate(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("glGenLists: negative count " + count);
        }
        if (count == 0) {
            return 0;
        }
        int first = nextName;
        nextName += count;
        for (int name = first; name < nextName; name++) {
            put(name, new Held(new ArrayList<>(), null));
        }
        return first;
    }

    public boolean exists(int name) {
        return held(name) != null;
    }

    public void begin(int name, int mode) {
        if (recording != null) {
            throw new IllegalStateException("glNewList called inside glNewList/glEndList");
        }
        if (name <= 0) {
            throw new IllegalArgumentException("glNewList: list names start at 1, got " + name);
        }
        if (mode != COMPILE && mode != COMPILE_AND_EXECUTE) {
            throw new IllegalArgumentException(
                    "glNewList: unknown mode 0x" + Integer.toHexString(mode));
        }
        recording = new ArrayList<>();
        recordingBlocks = new ArrayList<>();
        recordingInto = name;
        executeWhileRecording = mode == COMPILE_AND_EXECUTE;
        nextName = Math.max(nextName, name + 1);
    }

    public void end() {
        if (recording == null) {
            throw new IllegalStateException("glEndList called without glNewList");
        }
        free(recordingInto);
        put(recordingInto, new Held(recording, recordingBlocks));
        recording = null;
        recordingBlocks = null;
        executeWhileRecording = false;
    }

    public boolean isRecording() {
        return recording != null;
    }

    public boolean record(Runnable call) {
        if (recording == null) {
            return false;
        }
        recording.add(call);
        return !executeWhileRecording;
    }

    public void call(int name) {
        Held found = held(name);
        if (found == null) {
            return;
        }
        List<Runnable> commands = found.commands;
        for (int at = 0; at < commands.size(); at++) {
            commands.get(at).run();
        }
    }

    public void delete(int first, int count) {
        for (int name = first; name < first + count; name++) {
            free(name);
            if (name > 0 && name < lists.size()) {
                lists.set(name, null);
            }
        }
    }

    private Held held(int name) {
        return name > 0 && name < lists.size() ? lists.get(name) : null;
    }

    private void put(int name, Held into) {
        while (lists.size() <= name) {
            lists.add(null);
        }
        lists.set(name, into);
    }

    private void free(int name) {
        Held found = held(name);
        if (found == null || found.blocks == null) {
            return;
        }
        for (int block : found.blocks) {
            release.accept(block);
        }
        found.blocks = null;
    }
}
