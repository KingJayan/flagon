package dev.flagon.web.indexeddb;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSString;
import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBObjectStore;
import org.teavm.jso.indexeddb.IDBTransaction;
import org.teavm.jso.typedarrays.Int8Array;

import java.util.List;

/**
 * one object store, keyed by string, holding bytes or strings. each call is its own
 * transaction, and a write returns once it commits.
 */
public final class Store {

    private final IDBDatabase database;
    private final String name;

    Store(IDBDatabase database, String name) {
        this.database = database;
        this.name = name;
    }

    public String name() {
        return name;
    }

    /**
     * @return a copy of the bytes under the key, or null when there are none
     * @throws IllegalStateException when the key holds a string
     */
    public byte[] get(String key) {
        JSObject value = read(key);
        if (isMissing(value)) {
            return null;
        }
        if (!isBytes(value)) {
            throw new IllegalStateException(key + " in " + name + " holds no bytes");
        }
        return bytesOf(value).copyToJavaArray();
    }

    /**
     * @return the string under the key, or null when there is none
     * @throws IllegalStateException when the key holds bytes
     */
    public String getString(String key) {
        JSObject value = read(key);
        if (isMissing(value)) {
            return null;
        }
        if (!isString(value)) {
            throw new IllegalStateException(key + " in " + name + " holds no string");
        }
        return textOf(value);
    }

    public void put(String key, byte[] value) {
        IDBTransaction writing = writing();
        writing.objectStore(name).put(Int8Array.copyFromJavaArray(value), JSString.valueOf(key));
        Requests.committed(writing);
    }

    public void put(String key, String value) {
        IDBTransaction writing = writing();
        writing.objectStore(name).put(JSString.valueOf(value), JSString.valueOf(key));
        Requests.committed(writing);
    }

    public boolean has(String key) {
        return numberOf(Requests.result(reading().count(JSString.valueOf(key)))) > 0;
    }

    public void delete(String key) {
        IDBTransaction writing = writing();
        writing.objectStore(name).delete(JSString.valueOf(key));
        Requests.committed(writing);
    }

    /** @return every key, in indexeddb order */
    public List<String> keys() {
        return List.of(textsOf(Requests.result(reading().getAllKeys())));
    }

    public int size() {
        return numberOf(Requests.result(reading().count()));
    }

    public void clear() {
        IDBTransaction writing = writing();
        writing.objectStore(name).clear();
        Requests.committed(writing);
    }

    private JSObject read(String key) {
        return Requests.result(reading().get(JSString.valueOf(key)));
    }

    private IDBObjectStore reading() {
        return database.transaction(name, IDBDatabase.TRANSACTION_READONLY).objectStore(name);
    }

    private IDBTransaction writing() {
        return database.transaction(name, IDBDatabase.TRANSACTION_READWRITE);
    }

    @JSBody(params = "value", script = "return value === undefined;")
    private static native boolean isMissing(JSObject value);

    @JSBody(params = "value", script = "return ArrayBuffer.isView(value);")
    private static native boolean isBytes(JSObject value);

    @JSBody(params = "value", script = "return typeof value === 'string';")
    private static native boolean isString(JSObject value);

    @JSBody(params = "value", script = "return new Int8Array(value.buffer, value.byteOffset,"
            + " value.byteLength);")
    private static native Int8Array bytesOf(JSObject value);

    @JSBody(params = "value", script = "return value;")
    private static native String textOf(JSObject value);

    @JSBody(params = "value", script = "return value;")
    private static native int numberOf(JSObject value);

    @JSBody(params = "keys", script = "return keys.map(String);")
    private static native String[] textsOf(JSObject keys);
}
