package dev.flagon.platform.browser.graphics;

import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

final class GlNames {

    private final List<JSObject> objects = new ArrayList<>();
    private final BitSet free = new BitSet();
    private int[] freeNames = new int[16];
    private int head;
    private int tail;

    GlNames() {
        objects.add(null);
    }

    int add(JSObject object) {
        int reused = take();
        if (reused == 0) {
            objects.add(object);
            return objects.size() - 1;
        }
        objects.set(reused, object);
        return reused;
    }

    void put(int name, JSObject object) {
        if (name <= 0) {
            throw new IllegalArgumentException("not a gl name: " + name);
        }
        while (objects.size() <= name) {
            release(objects.size());
            objects.add(null);
        }
        free.clear(name);
        objects.set(name, object);
    }

    JSObject get(int name) {
        return name > 0 && name < objects.size() ? objects.get(name) : null;
    }

    void remove(int name) {
        if (get(name) == null) {
            return;
        }
        objects.set(name, null);
        release(name);
    }

    int size() {
        return objects.size();
    }

    private void release(int name) {
        if (free.get(name)) {
            return;
        }
        free.set(name);
        if (tail == freeNames.length) {
            compact();
        }
        freeNames[tail++] = name;
    }

    private int take() {
        while (head < tail) {
            int name = freeNames[head++];
            if (free.get(name)) {
                free.clear(name);
                return name;
            }
        }
        head = 0;
        tail = 0;
        return 0;
    }

    private void compact() {
        int held = tail - head;
        int[] into = held * 2 > freeNames.length ? new int[freeNames.length * 2] : freeNames;
        System.arraycopy(freeNames, head, into, 0, held);
        freeNames = into;
        head = 0;
        tail = held;
    }
}
