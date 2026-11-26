package dev.flagon.platform.fs;


import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * file contents on their way to the store. a store write suspends its thread, so a close inside
 * a synchronized block would let another thread find the lock held; handing the write over
 * keeps closes and flushes from ever suspending.
 */
final class WriteBehind {

    private final FileStore store;
    private final Executor writer;
    private final Map<String, byte[]> unwritten = new LinkedHashMap<>();
    private boolean writing;

    WriteBehind(FileStore store, Executor writer) {
        this.store = store;
        this.writer = writer;
    }

    /** the contents must not change after this; a later put for the same path replaces them */
    void put(String path, byte[] contents) {
        unwritten.put(path, contents);
        if (!writing) {
            writing = true;
            writer.execute(this::drain);
        }
    }

    /** what the store will hold once the queue is written, or null when it already does */
    byte[] get(String path) {
        return unwritten.get(path);
    }

    /** waits until everything queued is in the store */
    void settle() {
        while (writing) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void drain() {
        try {
            while (!unwritten.isEmpty()) {
                Iterator<Map.Entry<String, byte[]>> first = unwritten.entrySet().iterator();
                Map.Entry<String, byte[]> next = first.next();
                String path = next.getKey();
                byte[] contents = next.getValue();
                try {
                    store.write(path, contents);
                } catch (RuntimeException e) {
                    System.err.println("flagon: could not write " + path + ": " + e.getMessage());
                }
                unwritten.remove(path, contents);
            }
        } finally {
            writing = false;
        }
    }
}
