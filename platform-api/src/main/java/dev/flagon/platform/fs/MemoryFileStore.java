package dev.flagon.platform.fs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * a {@link FileStore} in the heap, for hosts with no storage of their own
 */
public final class MemoryFileStore implements FileStore {

    private static final byte[] NO_DATA = new byte[0];

    /** what one path holds. a directory keeps no bytes */
    private record Entry(byte[] bytes, long modified, boolean directory) {
    }

    private final Map<String, Entry> entries = new HashMap<>();

    public MemoryFileStore() {
        entries.put(FilePaths.ROOT, new Entry(NO_DATA, 0L, true));
    }

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public boolean exists(String path) {
        return entries.containsKey(FilePaths.normalize(path));
    }

    @Override
    public boolean isDirectory(String path) {
        return isDirectoryAt(FilePaths.normalize(path));
    }

    @Override
    public boolean isFile(String path) {
        return bytesAt(FilePaths.normalize(path)) != null;
    }

    @Override
    public String[] list(String path) {
        String at = FilePaths.normalize(path);
        if (!isDirectoryAt(at)) {
            return new String[0];
        }
        List<String> names = new ArrayList<>();
        for (String held : entries.keySet()) {
            if (!held.equals(FilePaths.ROOT) && FilePaths.parent(held).equals(at)) {
                names.add(FilePaths.name(held));
            }
        }
        return names.toArray(new String[0]);
    }

    @Override
    public int size(String path) {
        byte[] data = bytesAt(FilePaths.normalize(path));
        return data == null ? 0 : data.length;
    }

    @Override
    public long lastModified(String path) {
        Entry entry = entries.get(FilePaths.normalize(path));
        return entry == null ? 0L : entry.modified();
    }

    @Override
    public byte[] read(String path) {
        String at = FilePaths.normalize(path);
        byte[] data = bytesAt(at);
        if (data == null) {
            throw new FileStoreException("no such file: " + at);
        }
        return data.clone();
    }

    @Override
    public void write(String path, byte[] data) {
        String at = FilePaths.normalize(path);
        requireParent(at);
        if (isDirectoryAt(at)) {
            throw new FileStoreException("cannot write over a directory: " + at);
        }
        entries.put(at, new Entry(data.clone(), now(), false));
    }

    @Override
    public boolean createFile(String path) {
        String at = FilePaths.normalize(path);
        if (exists(at)) {
            return false;
        }
        requireParent(at);
        entries.put(at, new Entry(NO_DATA, now(), false));
        return true;
    }

    @Override
    public boolean createDirectory(String path) {
        String at = FilePaths.normalize(path);
        if (exists(at) || !isDirectoryAt(FilePaths.parent(at))) {
            return false;
        }
        entries.put(at, new Entry(NO_DATA, now(), true));
        return true;
    }

    @Override
    public boolean delete(String path) {
        String at = FilePaths.normalize(path);
        if (bytesAt(at) != null) {
            entries.remove(at);
            return true;
        }
        if (at.equals(FilePaths.ROOT) || !isDirectoryAt(at) || list(at).length > 0) {
            return false;
        }
        entries.remove(at);
        return true;
    }

    @Override
    public boolean move(String from, String to) {
        String source = FilePaths.normalize(from);
        String target = FilePaths.normalize(to);
        byte[] bytes = bytesAt(source);
        if (bytes == null || exists(target)
                || !isDirectoryAt(FilePaths.parent(target))) {
            return false;
        }
        entries.remove(source);
        entries.put(target, new Entry(bytes, now(), false));
        return true;
    }

    private boolean isDirectoryAt(String at) {
        Entry entry = entries.get(at);
        return entry != null && entry.directory();
    }

    private byte[] bytesAt(String at) {
        Entry entry = entries.get(at);
        return entry == null || entry.directory() ? null : entry.bytes();
    }

    private void requireParent(String path) {
        String parent = FilePaths.parent(path);
        if (!isDirectoryAt(parent)) {
            throw new FileStoreException("no such directory: " + parent);
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
