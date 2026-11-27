package dev.flagon.web.indexeddb;

import org.teavm.jso.JSBody;
import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBTransaction;

/** the stores of a database during its upgrade */
public final class Schema {

    private final IDBDatabase database;
    private final IDBTransaction transaction;

    Schema(IDBDatabase database, IDBTransaction transaction) {
        this.database = database;
        this.transaction = transaction;
    }

    public boolean has(String store) {
        return holds(database, store);
    }

    /** @return the store names, in order */
    public String[] stores() {
        return names(database);
    }

    public void create(String store) {
        database.createObjectStore(store);
    }

    public void delete(String store) {
        database.deleteObjectStore(store);
    }

    /** removes every entry of the store */
    public void clear(String store) {
        transaction.objectStore(store).clear();
    }

    @JSBody(params = { "database", "store" },
            script = "return database.objectStoreNames.contains(store);")
    static native boolean holds(IDBDatabase database, String store);

    @JSBody(params = "database", script = "return Array.from(database.objectStoreNames);")
    static native String[] names(IDBDatabase database);
}
