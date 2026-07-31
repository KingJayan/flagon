package dev.flagon.platform.browser.fs;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * the origin private file system
 */
final class Opfs {

    /** what a stat call returns; absent files answer with {@code exists} false rather than null. */
    interface Entry extends JSObject {
        @JSProperty
        boolean isExists();

        @JSProperty
        boolean isDirectory();

        @JSProperty
        int getSize();

        @JSProperty
        double getModified();
    }

    @JSFunctor
    interface Settled extends JSObject {
        void call(String error, JSObject result);
    }

    private Opfs() {
    }

    @JSBody(script = "return typeof navigator !== 'undefined' && !!navigator.storage "
            + "&& typeof navigator.storage.getDirectory === 'function';")
    static native boolean isAvailable();

    @Async
    static native Entry stat(String path);

    private static void stat(String path, AsyncCallback<Entry> callback) {
        opfsStat(path, settle(callback));
    }

    @Async
    static native JSArray<JSString> list(String path);

    private static void list(String path, AsyncCallback<JSArray<JSString>> callback) {
        opfsList(path, settle(callback));
    }

    @Async
    static native Int8Array read(String path);

    private static void read(String path, AsyncCallback<Int8Array> callback) {
        opfsRead(path, settle(callback));
    }

    @Async
    static native JSObject write(String path, Int8Array data);

    private static void write(String path, Int8Array data, AsyncCallback<JSObject> callback) {
        opfsWrite(path, data, settle(callback));
    }

    @Async
    static native JSObject create(String path, boolean directory);

    private static void create(String path, boolean directory, AsyncCallback<JSObject> callback) {
        opfsCreate(path, directory, settle(callback));
    }

    @Async
    static native JSObject delete(String path);

    private static void delete(String path, AsyncCallback<JSObject> callback) {
        opfsDelete(path, settle(callback));
    }

    @Async
    static native JSObject move(String from, String to);

    private static void move(String from, String to, AsyncCallback<JSObject> callback) {
        opfsMove(from, to, settle(callback));
    }

    /**
     * a rejected promise arrives as a message rather than an exception object: the javascript
     * side may throw anything at all, and only its text survives the boundary intact.
     */
    private static <T extends JSObject> Settled settle(AsyncCallback<T> callback) {
        return (error, result) -> {
            if (error != null) {
                callback.error(new dev.flagon.platform.fs.FileStoreException(error));
            } else {
                @SuppressWarnings("unchecked")
                T typed = (T) result;
                callback.complete(typed);
            }
        };
    }

    @JSBody(script = """
            if (window.__flagonOpfs) { return; }
            const walk = async (path, createDirectories) => {
                let directory = await navigator.storage.getDirectory();
                const parts = path.split('/').filter(part => part.length > 0);
                const name = parts.pop();
                for (const part of parts) {
                    directory = await directory.getDirectoryHandle(part, { create: createDirectories });
                }
                return { directory, name };
            };
            const settle = (promise, done) => promise.then(
                value => done(null, value === undefined ? null : value),
                error => done(String(error && error.message ? error.message : error), null));
            window.__flagonOpfs = {
                stat: (path, done) => settle((async () => {
                    const root = await navigator.storage.getDirectory();
                    if (path === '/') {
                        return { exists: true, directory: true, size: 0, modified: 0 };
                    }
                    const { directory, name } = await walk(path, false);
                    try {
                        const handle = await directory.getFileHandle(name);
                        const file = await handle.getFile();
                        return { exists: true, directory: false, size: file.size,
                                 modified: file.lastModified };
                    } catch (missingFile) {
                        try {
                            await directory.getDirectoryHandle(name);
                            return { exists: true, directory: true, size: 0, modified: 0 };
                        } catch (missingDirectory) {
                            return { exists: false, directory: false, size: 0, modified: 0 };
                        }
                    }
                })(), done),
                list: (path, done) => settle((async () => {
                    let directory = await navigator.storage.getDirectory();
                    for (const part of path.split('/').filter(part => part.length > 0)) {
                        directory = await directory.getDirectoryHandle(part);
                    }
                    const names = [];
                    for await (const name of directory.keys()) { names.push(name); }
                    return names;
                })(), done),
                read: (path, done) => settle((async () => {
                    const { directory, name } = await walk(path, false);
                    const handle = await directory.getFileHandle(name);
                    const file = await handle.getFile();
                    return new Int8Array(await file.arrayBuffer());
                })(), done),
                write: (path, data, done) => settle((async () => {
                    const { directory, name } = await walk(path, false);
                    const handle = await directory.getFileHandle(name, { create: true });
                    const writable = await handle.createWritable();
                    await writable.write(data);
                    await writable.close();
                })(), done),
                create: (path, isDirectory, done) => settle((async () => {
                    const { directory, name } = await walk(path, false);
                    if (isDirectory) {
                        await directory.getDirectoryHandle(name, { create: true });
                    } else {
                        await directory.getFileHandle(name, { create: true });
                    }
                })(), done),
                remove: (path, done) => settle((async () => {
                    const { directory, name } = await walk(path, false);
                    await directory.removeEntry(name);
                })(), done),
                move: (from, to, done) => settle((async () => {
                    const source = await walk(from, false);
                    const target = await walk(to, false);
                    const handle = await source.directory.getFileHandle(source.name);
                    if (typeof handle.move === 'function') {
                        await handle.move(target.directory, target.name);
                        return;
                    }
                    const file = await handle.getFile();
                    const copy = await target.directory.getFileHandle(target.name, { create: true });
                    const writable = await copy.createWritable();
                    await writable.write(await file.arrayBuffer());
                    await writable.close();
                    await source.directory.removeEntry(source.name);
                })(), done),
            };
            """)
    static native void install();

    @JSBody(params = {"path", "done"}, script = "window.__flagonOpfs.stat(path, done);")
    private static native void opfsStat(String path, Settled done);

    @JSBody(params = {"path", "done"}, script = "window.__flagonOpfs.list(path, done);")
    private static native void opfsList(String path, Settled done);

    @JSBody(params = {"path", "done"}, script = "window.__flagonOpfs.read(path, done);")
    private static native void opfsRead(String path, Settled done);

    @JSBody(params = {"path", "data", "done"}, script = "window.__flagonOpfs.write(path, data, done);")
    private static native void opfsWrite(String path, Int8Array data, Settled done);

    @JSBody(params = {"path", "directory", "done"},
            script = "window.__flagonOpfs.create(path, directory, done);")
    private static native void opfsCreate(String path, boolean directory, Settled done);

    @JSBody(params = {"path", "done"}, script = "window.__flagonOpfs.remove(path, done);")
    private static native void opfsDelete(String path, Settled done);

    @JSBody(params = {"from", "to", "done"}, script = "window.__flagonOpfs.move(from, to, done);")
    private static native void opfsMove(String from, String to, Settled done);
}
