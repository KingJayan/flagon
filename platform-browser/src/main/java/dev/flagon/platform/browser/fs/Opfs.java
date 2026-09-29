package dev.flagon.platform.browser.fs;

import dev.flagon.platform.fs.FileStoreException;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

final class Opfs {

    private final Map<String, Directory> directories = new HashMap<>();

    private Directory root;

    interface Entry extends JSObject {
        @JSProperty
        boolean isExists();

        @JSProperty
        void setExists(boolean exists);

        @JSProperty
        boolean isDirectory();

        @JSProperty
        void setDirectory(boolean directory);

        @JSProperty
        int getSize();

        @JSProperty
        void setSize(int size);

        @JSProperty
        double getModified();

        @JSProperty
        void setModified(double modified);
    }

    interface Storage extends JSObject {
        JSPromise<Directory> getDirectory();
    }

    interface Creating extends JSObject {
        @JSProperty
        void setCreate(boolean wanted);
    }

    interface Directory extends JSObject {
        JSPromise<Directory> getDirectoryHandle(String name);

        JSPromise<Directory> getDirectoryHandle(String name, Creating options);

        JSPromise<Handle> getFileHandle(String name);

        JSPromise<Handle> getFileHandle(String name, Creating options);

        JSPromise<JSObject> removeEntry(String name);

        Names keys();
    }

    interface Handle extends JSObject {
        JSPromise<Contents> getFile();

        JSPromise<Writable> createWritable();

        JSPromise<JSObject> move(Directory directory, String name);
    }

    interface Contents extends JSObject {
        @JSProperty
        int getSize();

        @JSProperty
        double getLastModified();

        JSPromise<ArrayBuffer> arrayBuffer();
    }

    interface Writable extends JSObject {
        JSPromise<JSObject> write(JSObject data);

        JSPromise<JSObject> close();
    }

    interface Names extends JSObject {
        JSPromise<Step> next();
    }

    interface Step extends JSObject {
        @JSProperty
        boolean isDone();

        @JSProperty
        JSString getValue();
    }

    private record At(Directory directory, String name) {
    }

    Opfs() {
    }

    static boolean isAvailable() {
        return hasNavigator() && hasStorage() && offersDirectory();
    }

    Entry stat(String path) {
        if ("/".equals(path)) {
            return entry(true, true, 0, 0);
        }

        String[] names = parts(path);
        if (names.length == 0) {
            return missing();
        }

        Directory directory = above(names);
        if (directory == null) {
            return missing();
        }

        String name = names[names.length - 1];
        Handle handle = orNull(directory.getFileHandle(name));
        if (handle != null) {
            Contents file = orNull(handle.getFile());
            if (file != null) {
                return entry(true, false, file.getSize(), file.getLastModified());
            }
        }
        return orNull(directory.getDirectoryHandle(name)) == null ? missing()
                : entry(true, true, 0, 0);
    }

    private Directory above(String[] names) {
        Directory at = root();
        for (int name = 0; at != null && name < names.length - 1; name++) {
            String key = key(names, name + 1);
            Directory known = directories.get(key);
            if (known == null) {
                known = orNull(at.getDirectoryHandle(names[name]));
                if (known != null) {
                    directories.put(key, known);
                }
            }
            at = known;
        }
        return at;
    }

    JSArray<JSString> list(String path) {
        String[] names = parts(path);
        Directory directory = descend(names, names.length);
        JSArray<JSString> found = JSArray.create();
        Names walking = directory.keys();
        for (Step step = settled(walking.next()); !step.isDone(); step = settled(walking.next())) {
            found.push(step.getValue());
        }
        return found;
    }

    Int8Array read(String path) {
        At at = walk(path);
        Handle handle = settled(at.directory().getFileHandle(at.name()));
        return new Int8Array(settled(settled(handle.getFile()).arrayBuffer()));
    }

    JSObject write(String path, Int8Array data) {
        At at = walk(path);
        Handle handle = settled(at.directory().getFileHandle(at.name(), creating()));
        return put(handle, data);
    }

    JSObject create(String path, boolean directory) {
        At at = walk(path);
        return directory
                ? settled(at.directory().getDirectoryHandle(at.name(), creating())).cast()
                : settled(at.directory().getFileHandle(at.name(), creating())).cast();
    }

    JSObject delete(String path) {
        At at = walk(path);
        JSObject done = settled(at.directory().removeEntry(at.name()));
        forget(path);
        return done;
    }

    JSObject move(String from, String to) {
        At source = walk(from);
        At target = walk(to);
        forget(from);
        forget(to);
        Handle handle = settled(source.directory().getFileHandle(source.name()));
        if (movesItself(handle)) {
            return settled(handle.move(target.directory(), target.name()));
        }

        ArrayBuffer body = settled(settled(handle.getFile()).arrayBuffer());
        put(settled(target.directory().getFileHandle(target.name(), creating())), body);
        return settled(source.directory().removeEntry(source.name()));
    }

    private static JSObject put(Handle handle, JSObject body) {
        Writable writable = settled(handle.createWritable());
        settled(writable.write(body));
        return settled(writable.close());
    }

    private At walk(String path) {
        String[] names = parts(path);
        return names.length == 0
                ? new At(descend(names, 0), "")
                : new At(descend(names, names.length - 1), names[names.length - 1]);
    }

    private Directory descend(String[] names, int count) {
        Directory at = root();
        for (int name = 0; name < count; name++) {
            String key = key(names, name + 1);
            Directory known = directories.get(key);
            if (known == null) {
                known = settled(at.getDirectoryHandle(names[name]));
                directories.put(key, known);
            }
            at = known;
        }
        return at;
    }

    private Directory root() {
        if (root == null) {
            root = settled(storage().getDirectory());
        }
        return root;
    }

    private static String key(String[] names, int count) {
        StringBuilder key = new StringBuilder();
        for (int name = 0; name < count; name++) {
            key.append(names[name]).append('/');
        }
        return key.toString();
    }

    private void forget(String path) {
        String gone = key(parts(path), parts(path).length);
        directories.keySet().removeIf(held -> held.equals(gone) || held.startsWith(gone));
    }

    private static String[] parts(String path) {
        List<String> kept = new ArrayList<>();
        for (String name : path.split("/")) {
            if (!name.isEmpty()) {
                kept.add(name);
            }
        }
        return kept.toArray(new String[0]);
    }

    private static Entry missing() {
        return entry(false, false, 0, 0);
    }

    private static Entry entry(boolean exists, boolean directory, int size, double modified) {
        Entry made = JSObjects.create();
        made.setExists(exists);
        made.setDirectory(directory);
        made.setSize(size);
        made.setModified(modified);
        return made;
    }

    private static Creating creating() {
        Creating wanted = JSObjects.create();
        wanted.setCreate(true);
        return wanted;
    }

    @JSBody(script = "return navigator.storage;")
    private static native Storage storage();

    private static <T extends JSObject> T orNull(JSPromise<T> promise) {
        return settled(promise.catchError(refused -> null));
    }

    @Async
    private static native <T extends JSObject> T settled(JSPromise<T> promise);

    private static <T extends JSObject> void settled(JSPromise<T> promise,
            AsyncCallback<T> callback) {
        promise.then(value -> {
            callback.complete(JSObjects.isUndefined(value) ? null : value);
            return null;
        }, error -> {
            callback.error(new FileStoreException(describe(error)));
            return null;
        });
    }

    private static String describe(Object error) {
        JSObject thrown = (JSObject) error;
        return thrown != null && hasMessage(thrown) ? messageOf(thrown) : textOf(thrown);
    }

    @JSBody(script = "return typeof navigator !== 'undefined';")
    private static native boolean hasNavigator();

    @JSBody(script = "return navigator.storage != null;")
    private static native boolean hasStorage();

    @JSBody(script = "return typeof navigator.storage.getDirectory === 'function';")
    private static native boolean offersDirectory();

    @JSBody(params = "error", script = "return typeof error.message === 'string';")
    private static native boolean hasMessage(JSObject error);

    @JSBody(params = "error", script = "return error.message;")
    private static native String messageOf(JSObject error);

    @JSBody(params = "error", script = "return String(error);")
    private static native String textOf(JSObject error);

    @JSBody(params = "handle", script = "return typeof handle.move === 'function';")
    private static native boolean movesItself(Handle handle);
}
