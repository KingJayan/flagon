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

    private final Map<String, byte[]> files = new HashMap<>();
    private final Map<String, Long> modified = new HashMap<>();
    private final Map<String, Boolean> directories = new HashMap<>();

    public MemoryFileStore() {
        directories.put(FilePaths.ROOT, Boolean.TRUE);
    }

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public boolean exists(String path) {
        String at = FilePaths.normalize(path);
        return files.containsKey(at) || directories.containsKey(at);
    }

    @Override
    public boolean isDirectory(String path) {
        return directories.containsKey(FilePaths.normalize(path));
    }

    @Override
    public boolean isFile(String path) {
        return files.containsKey(FilePaths.normalize(path));
    }

    @Override
    public String[] list(String path) {
        String at = FilePaths.normalize(path);
        if (!directories.containsKey(at)) {
            return new String[0];
        }
        List<String> names = new ArrayList<>();
        collectChildren(at, files.keySet(), names);
        collectChildren(at, directories.keySet(), names);
        return names.toArray(new String[0]);
    }

    private static void collectChildren(String parent, Iterable<String> paths, List<String> into) {
        for (String path : paths) {
            if (!path.equals(FilePaths.ROOT) && FilePaths.parent(path).equals(parent)) {
                into.add(FilePaths.name(path));
            }
        }
    }

    @Override
    public int size(String path) {
        byte[] data = files.get(FilePaths.normalize(path));
        return data == null ? 0 : data.length;
    }

    @Override
    public long lastModified(String path) {
        Long at = modified.get(FilePaths.normalize(path));
        return at == null ? 0L : at;
    }

    @Override
    public byte[] read(String path) {
        byte[] data = files.get(FilePaths.normalize(path));
        if (data == null) {
            throw new FileStoreException("no such file: " + FilePaths.normalize(path));
        }
        return data.clone();
    }

    @Override
    public void write(String path, byte[] data) {
        String at = FilePaths.normalize(path);
        requireParent(at);
        if (directories.containsKey(at)) {
            throw new FileStoreException("cannot write over a directory: " + at);
        }
        files.put(at, data.clone());
        modified.put(at, now());
    }

    @Override
    public boolean createFile(String path) {
        String at = FilePaths.normalize(path);
        if (exists(at)) {
            return false;
        }
        requireParent(at);
        files.put(at, NO_DATA);
        modified.put(at, now());
        return true;
    }

    @Override
    public boolean createDirectory(String path) {
        String at = FilePaths.normalize(path);
        if (exists(at) || !directories.containsKey(FilePaths.parent(at))) {
            return false;
        }
        directories.put(at, Boolean.TRUE);
        modified.put(at, now());
        return true;
    }

    @Override
    public boolean delete(String path) {
        String at = FilePaths.normalize(path);
        if (files.remove(at) != null) {
            modified.remove(at);
            return true;
        }
        if (at.equals(FilePaths.ROOT) || !directories.containsKey(at) || list(at).length > 0) {
            return false;
        }
        directories.remove(at);
        modified.remove(at);
        return true;
    }

    @Override
    public boolean move(String from, String to) {
        String source = FilePaths.normalize(from);
        String target = FilePaths.normalize(to);
        if (!files.containsKey(source) || exists(target)
                || !directories.containsKey(FilePaths.parent(target))) {
            return false;
        }
        files.put(target, files.remove(source));
        modified.put(target, now());
        modified.remove(source);
        return true;
    }

    private void requireParent(String path) {
        String parent = FilePaths.parent(path);
        if (!directories.containsKey(parent)) {
            throw new FileStoreException("no such directory: " + parent);
        }
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
