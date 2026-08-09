package dev.flagon.lwjgl.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * gl's display lists, as recorded commands
 */
public final class DisplayLists {

    public static final int COMPILE = 0x1300;
    public static final int COMPILE_AND_EXECUTE = 0x1301;

    private final Map<Integer, List<Runnable>> lists = new HashMap<>();
    private List<Runnable> recording;
    private int recordingInto;
    private boolean executeWhileRecording;
    private int nextName = 1;

    /** reserves {@code count} consecutive names, as {@code glGenLists} does. */
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
            lists.put(name, new ArrayList<>());
        }
        return first;
    }

    public boolean exists(int name) {
        return lists.containsKey(name);
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
        recordingInto = name;
        executeWhileRecording = mode == COMPILE_AND_EXECUTE;
        nextName = Math.max(nextName, name + 1);
    }

    public void end() {
        if (recording == null) {
            throw new IllegalStateException("glEndList called without glNewList");
        }
        lists.put(recordingInto, recording);
        recording = null;
        executeWhileRecording = false;
    }

    public boolean isRecording() {
        return recording != null;
    }

    /**
     * @return true when the call was recorded and must not also run now
     */
    public boolean record(Runnable call) {
        if (recording == null) {
            return false;
        }
        recording.add(call);
        return !executeWhileRecording;
    }

    public void call(int name) {
        List<Runnable> list = lists.get(name);
        if (list == null) {
            // gl ignores a call to a list that was never built
            return;
        }
        for (Runnable command : list) {
            command.run();
        }
    }

    /** deletes {@code count} names from {@code first}, as {@code glDeleteLists} does. */
    public void delete(int first, int count) {
        for (int name = first; name < first + count; name++) {
            lists.remove(name);
        }
    }
}
