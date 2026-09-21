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

    static boolean isAvailable() {
        return hasNavigator() && hasStorage() && offersDirectory();
    }

    @JSBody(script = "return typeof navigator !== 'undefined';")
    private static native boolean hasNavigator();

    @JSBody(script = "return navigator.storage != null;")
    private static native boolean hasStorage();

    @JSBody(script = "return typeof navigator.storage.getDirectory === 'function';")
    private static native boolean offersDirectory();

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

    /**
     * written without {@code async}, {@code await} or arrow functions on purpose: teavm parses
     * every {@code @JSBody} script, and its parser predates all three. the browser would run the
     * shorter form, but a script the compiler cannot read is one it cannot process.
     */
    @JSBody(script = """
            if (window.__flagonOpfs) { return; }
            var root = function() { return navigator.storage.getDirectory(); };
            var parts = function(path) {
                var all = path.split('/');
                var kept = [];
                for (var at = 0; at < all.length; at++) {
                    if (all[at].length > 0) { kept.push(all[at]); }
                }
                return kept;
            };
            var descend = function(directory, names, at) {
                if (at >= names.length) { return Promise.resolve(directory); }
                return directory.getDirectoryHandle(names[at]).then(function(child) {
                    return descend(child, names, at + 1);
                });
            };
            var walk = function(path) {
                var names = parts(path);
                var name = names.pop();
                return root().then(function(directory) {
                    return descend(directory, names, 0);
                }).then(function(directory) {
                    return { directory: directory, name: name };
                });
            };
            var settle = function(promise, done) {
                promise.then(function(value) {
                    done(null, value === undefined ? null : value);
                }, function(error) {
                    done(String(error && error.message ? error.message : error), null);
                });
            };
            var collect = function(iterator, names) {
                return iterator.next().then(function(step) {
                    if (step.done) { return names; }
                    names.push(step.value);
                    return collect(iterator, names);
                });
            };
            var absent = { exists: false, directory: false, size: 0, modified: 0 };
            var directoryEntry = { exists: true, directory: true, size: 0, modified: 0 };
            window.__flagonOpfs = {
                stat: function(path, done) {
                    if (path === '/') { done(null, directoryEntry); return; }
                    settle(walk(path).then(function(at) {
                        return at.directory.getFileHandle(at.name).then(function(handle) {
                            return handle.getFile().then(function(file) {
                                return { exists: true, directory: false, size: file.size,
                                         modified: file.lastModified };
                            });
                        }, function() {
                            return at.directory.getDirectoryHandle(at.name).then(function() {
                                return directoryEntry;
                            }, function() { return absent; });
                        });
                    // a path whose parent does not exist is a path that does not exist, which is
                    // what File.exists() asks about and what it expects to hear back
                    }, function() { return absent; }), done);
                },
                list: function(path, done) {
                    settle(root().then(function(directory) {
                        return descend(directory, parts(path), 0);
                    }).then(function(directory) {
                        return collect(directory.keys(), []);
                    }), done);
                },
                read: function(path, done) {
                    settle(walk(path).then(function(at) {
                        return at.directory.getFileHandle(at.name);
                    }).then(function(handle) {
                        return handle.getFile();
                    }).then(function(file) {
                        return file.arrayBuffer();
                    }).then(function(buffer) {
                        return new Int8Array(buffer);
                    }), done);
                },
                write: function(path, data, done) {
                    settle(walk(path).then(function(at) {
                        return at.directory.getFileHandle(at.name, { create: true });
                    }).then(function(handle) {
                        return handle.createWritable();
                    }).then(function(writable) {
                        return writable.write(data).then(function() {
                            return writable.close();
                        });
                    }), done);
                },
                create: function(path, isDirectory, done) {
                    settle(walk(path).then(function(at) {
                        return isDirectory
                            ? at.directory.getDirectoryHandle(at.name, { create: true })
                            : at.directory.getFileHandle(at.name, { create: true });
                    }), done);
                },
                remove: function(path, done) {
                    settle(walk(path).then(function(at) {
                        return at.directory.removeEntry(at.name);
                    }), done);
                },
                move: function(from, to, done) {
                    settle(walk(from).then(function(source) {
                        return walk(to).then(function(target) {
                            return source.directory.getFileHandle(source.name)
                                    .then(function(handle) {
                                if (typeof handle.move === 'function') {
                                    return handle.move(target.directory, target.name);
                                }
                                // chrome gained FileSystemHandle.move late; everywhere else a
                                // rename is a copy followed by a delete
                                return handle.getFile().then(function(file) {
                                    return file.arrayBuffer();
                                }).then(function(buffer) {
                                    return target.directory
                                            .getFileHandle(target.name, { create: true })
                                            .then(function(copy) {
                                        return copy.createWritable();
                                    }).then(function(writable) {
                                        return writable.write(buffer).then(function() {
                                            return writable.close();
                                        });
                                    });
                                }).then(function() {
                                    return source.directory.removeEntry(source.name);
                                });
                            });
                        });
                    }), done);
                }
            };
            """)
    static native void install();

    @JSBody(params = {"path", "done"}, script = "window.__flagonOpfs.stat(path, done);")
    private static native void opfsStat(String path, Settled done);

    @JSBody(params = {"path", "done"}, script = "window.__flagonOpfs.list(path, done);")
    private static native void opfsList(String path, Settled done);

    @JSBody(params = {"path", "done"}, script = "window.__flagonOpfs.read(path, done);")
    private static native void opfsRead(String path, Settled done);

    @JSBody(params = {"path", "data", "done"},
            script = "window.__flagonOpfs.write(path, data, done);")
    private static native void opfsWrite(String path, Int8Array data, Settled done);

    @JSBody(params = {"path", "directory", "done"},
            script = "window.__flagonOpfs.create(path, directory, done);")
    private static native void opfsCreate(String path, boolean directory, Settled done);

    @JSBody(params = {"path", "done"}, script = "window.__flagonOpfs.remove(path, done);")
    private static native void opfsDelete(String path, Settled done);

    @JSBody(params = {"from", "to", "done"}, script = "window.__flagonOpfs.move(from, to, done);")
    private static native void opfsMove(String from, String to, Settled done);
}
