package dev.flagon.platform.browser.graphics;

import org.teavm.jso.JSObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * gl names are ints and webgl hands back objects, so a table maps between them
 *
 * <p>index 0 stays empty so 0 means "none", matching gl's convention. a deleted name is handed out
 * again, as gl does, so a session that churns objects — a chunk's buffers rebuilt every time it
 * changes — reuses the table rather than growing it for as long as the page is open.
 */
final class GlNames {

    private final List<JSObject> objects = new ArrayList<>();
    private final Deque<Integer> free = new ArrayDeque<>();

    GlNames() {
        objects.add(null);
    }

    int add(JSObject object) {
        Integer reused = free.poll();
        if (reused == null) {
            objects.add(object);
            return objects.size() - 1;
        }
        objects.set(reused, object);
        return reused;
    }

    /** @return what a name stands for, or null if it stands for nothing live */
    JSObject get(int name) {
        return name > 0 && name < objects.size() ? objects.get(name) : null;
    }

    void remove(int name) {
        if (get(name) == null) {
            return;
        }
        objects.set(name, null);
        free.add(name);
    }

    /** how many names exist at all, live or free: the number that must not grow without bound */
    int size() {
        return objects.size();
    }
}
