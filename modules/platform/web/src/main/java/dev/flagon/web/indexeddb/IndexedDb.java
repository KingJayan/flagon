package dev.flagon.web.indexeddb;

import dev.flagon.web.WebException;

import org.teavm.jso.JSBody;
import org.teavm.jso.indexeddb.IDBDatabase;
import org.teavm.jso.indexeddb.IDBFactory;
import org.teavm.jso.indexeddb.IDBOpenDBRequest;
import org.teavm.jso.indexeddb.IDBTransaction;

/**
 * an indexeddb database. each call parks the java thread until the browser answers, and the
 * page keeps running.
 */
public final class IndexedDb implements AutoCloseable {

    /** changes the stores when the database is new or older than the version asked for */
    @FunctionalInterface
    public interface Upgrade {
        /**
         * @param from the version the database had, 0 when it is new
         * @param to the version it opens at
         */
        void upgrade(Schema schema, int from, int to);
    }

    private final IDBDatabase database;

    private IndexedDb(IDBDatabase database) {
        this.database = database;
        // let another tab open a newer version instead of blocking it
        database.setOnVersionChange(database::close);
    }

    public static boolean isSupported() {
        return hasIndexedDb();
    }

    /** opens the database, and makes each of the stores it does not have yet */
    public static IndexedDb open(String name, int version, String... stores) {
        return open(name, version, (schema, from, to) -> {
            for (String store : stores) {
                if (!schema.has(store)) {
                    schema.create(store);
                }
            }
        });
    }

    /**
     * opens the database, and runs the upgrade first when its version is below the one asked
     * for. an exception from the upgrade aborts it and is thrown here.
     */
    public static IndexedDb open(String name, int version, Upgrade upgrade) {
        IDBOpenDBRequest request = factory().open(name, version);
        RuntimeException[] failed = new RuntimeException[1];
        request.setOnUpgradeNeeded(event -> {
            IDBTransaction changing = request.getTransaction();
            try {
                upgrade.upgrade(new Schema(request.getResult(), changing),
                        event.getOldVersion(), event.getNewVersion());
            } catch (RuntimeException thrown) {
                failed[0] = thrown;
                changing.abort();
            }
        });
        try {
            return new IndexedDb((IDBDatabase) Requests.result(request));
        } catch (RuntimeException refused) {
            throw failed[0] != null ? failed[0] : refused;
        }
    }

    /** deletes the database once every other connection to it closes */
    public static void delete(String name) {
        Requests.result(factory().deleteDatabase(name));
    }

    public String name() {
        return database.getName();
    }

    public int version() {
        return database.getVersion();
    }

    /** @return the database's store names, in order */
    public String[] stores() {
        return Schema.names(database);
    }

    /** @throws WebException NotFoundError when the store does not exist */
    public Store store(String name) {
        if (!Schema.holds(database, name)) {
            throw new WebException("NotFoundError", this.name() + " has no store " + name);
        }
        return new Store(database, name);
    }

    @Override
    public void close() {
        database.close();
    }

    private static IDBFactory factory() {
        if (!hasIndexedDb()) {
            throw new UnsupportedOperationException("this browser has no indexeddb");
        }
        return indexedDb();
    }

    @JSBody(script = "return indexedDB;")
    private static native IDBFactory indexedDb();

    @JSBody(script = "return typeof indexedDB !== 'undefined';")
    private static native boolean hasIndexedDb();
}
