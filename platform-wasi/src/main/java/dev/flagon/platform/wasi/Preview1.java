package dev.flagon.platform.wasi;

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.runtime.heap.Heap;
import org.teavm.runtime.wasi.Wasi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** the wasi preview 1 file calls, on paths below one preopened directory */
final class Preview1 {

    static final int BAD_DESCRIPTOR = 8;
    static final int EXISTS = 20;
    static final int NO_ENTRY = 44;

    static final int DIRECTORY = 3;
    static final int REGULAR_FILE = 4;

    private static final int CREATE = 1;
    private static final int ONLY_DIRECTORY = 2;
    private static final int EXCLUSIVE = 4;
    private static final int TRUNCATE = 8;
    private static final int FOLLOW_LINKS = 1;
    // a descriptor asks only for what it is opened to do; write rights refuse a directory
    private static final long READ = 1L << 1;
    private static final long WRITE = 1L << 6;
    private static final long READ_DIRECTORY = 1L << 14;

    private static final int STAT = 64;
    private static final int CHUNK = 4096;
    private static final int ENTRY_HEADER = 24;

    private static Address scratch;

    private Preview1() {
    }

    private static Address scratch() {
        if (scratch == null) {
            scratch = Heap.alloc(STAT);
        }
        return scratch;
    }

    /** @return the first preopened directory, or -1 when the host gave none */
    static int preopen() {
        for (int fd = 3; ; fd++) {
            int errno = fdPrestatGet(fd, scratch());
            if (errno == BAD_DESCRIPTOR) {
                return -1;
            }
            if (errno == 0 && scratch().getByte() == 0) {
                return fd;
            }
        }
    }

    /** a file's type, size and modification time, from its status */
    record Status(int type, long size, long modifiedMillis) {
    }

    /** @return what is at the path, or null when nothing is */
    static Status status(int directory, String path) {
        Address name = text(path);
        Address stat = scratch();
        int errno = pathFilestatGet(directory, FOLLOW_LINKS, name, length(path), stat);
        Heap.release(name);
        if (errno != 0) {
            return null;
        }
        return new Status(stat.add(16).getByte(), stat.add(32).getLong(),
                stat.add(48).getLong() / 1_000_000L);
    }

    /** @return the whole file, or null when it cannot be opened */
    static byte[] read(int directory, String path) {
        int fd = open(directory, path, 0, READ);
        if (fd < 0) {
            return null;
        }
        byte[] data = new byte[CHUNK];
        int length = 0;
        while (true) {
            if (length == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            int count = Wasi.read(fd, data, length, data.length - length);
            if (count <= 0) {
                fdClose(fd);
                return count < 0 ? null : Arrays.copyOf(data, length);
            }
            length += count;
        }
    }

    /** @return 0, or the wasi errno that stopped the write */
    static int write(int directory, String path, byte[] data, boolean exclusive) {
        int fd = open(directory, path, exclusive ? CREATE | EXCLUSIVE : CREATE | TRUNCATE, WRITE);
        if (fd < 0) {
            return -fd;
        }
        int written = 0;
        int errno = 0;
        while (written < data.length) {
            int count = Wasi.write(fd, data, written, data.length - written);
            if (count < 0) {
                errno = -count;
                break;
            }
            written += count;
        }
        fdClose(fd);
        return errno;
    }

    /** @return the names in the directory, or null when it cannot be opened */
    static String[] list(int directory, String path) {
        int fd = open(directory, path, ONLY_DIRECTORY, READ | READ_DIRECTORY);
        if (fd < 0) {
            return null;
        }
        List<String> names = new ArrayList<>();
        Address buffer = Heap.alloc(CHUNK);
        long cookie = 0;
        while (true) {
            if (fdReaddir(fd, buffer, CHUNK, cookie, scratch()) != 0) {
                break;
            }
            int used = scratch().getInt();
            int at = 0;
            while (at + ENTRY_HEADER <= used) {
                Address entry = buffer.add(at);
                int nameLength = entry.add(16).getInt();
                if (at + ENTRY_HEADER + nameLength > used) {
                    break;
                }
                String name = decode(entry.add(ENTRY_HEADER), nameLength);
                if (!name.equals(".") && !name.equals("..")) {
                    names.add(name);
                }
                cookie = entry.getLong();
                at += ENTRY_HEADER + nameLength;
            }
            // a short buffer is the last; an empty one could not hold the next name
            if (used < CHUNK || at == 0) {
                break;
            }
        }
        Heap.release(buffer);
        fdClose(fd);
        return names.toArray(new String[0]);
    }

    static int createDirectory(int directory, String path) {
        return onPath(directory, path, Preview1::pathCreateDirectory);
    }

    static int unlink(int directory, String path) {
        return onPath(directory, path, Preview1::pathUnlinkFile);
    }

    static int removeDirectory(int directory, String path) {
        return onPath(directory, path, Preview1::pathRemoveDirectory);
    }

    static int rename(int directory, String from, String to) {
        Address source = text(from);
        Address target = text(to);
        int errno = pathRename(directory, source, length(from), directory, target, length(to));
        Heap.release(source);
        Heap.release(target);
        return errno;
    }

    private interface PathCall {
        int on(int directory, Address path, int length);
    }

    private static int onPath(int directory, String path, PathCall call) {
        Address name = text(path);
        int errno = call.on(directory, name, length(path));
        Heap.release(name);
        return errno;
    }

    /** @return the opened descriptor, or a negative wasi errno */
    private static int open(int directory, String path, int flags, long rights) {
        Address name = text(path);
        Address opened = scratch();
        int errno = pathOpen(directory, FOLLOW_LINKS, name, length(path), flags, rights, rights, 0,
                opened);
        Heap.release(name);
        return errno == 0 ? opened.getInt() : -errno;
    }

    private static Address text(String path) {
        byte[] bytes = path.getBytes(StandardCharsets.UTF_8);
        Address copy = Heap.alloc(Math.max(bytes.length, 1));
        for (int i = 0; i < bytes.length; i++) {
            copy.add(i).putByte(bytes[i]);
        }
        return copy;
    }

    private static int length(String path) {
        return path.getBytes(StandardCharsets.UTF_8).length;
    }

    private static String decode(Address start, int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = start.add(i).getByte();
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Import(module = "wasi_snapshot_preview1", name = "fd_prestat_get")
    private static native int fdPrestatGet(int fd, Address prestat);

    @Import(module = "wasi_snapshot_preview1", name = "fd_close")
    private static native int fdClose(int fd);

    @Import(module = "wasi_snapshot_preview1", name = "fd_readdir")
    private static native int fdReaddir(int fd, Address buffer, int length, long cookie,
            Address used);

    @Import(module = "wasi_snapshot_preview1", name = "path_open")
    private static native int pathOpen(int fd, int lookup, Address path, int length, int flags,
            long rights, long inherited, int descriptorFlags, Address opened);

    @Import(module = "wasi_snapshot_preview1", name = "path_filestat_get")
    private static native int pathFilestatGet(int fd, int lookup, Address path, int length,
            Address stat);

    @Import(module = "wasi_snapshot_preview1", name = "path_create_directory")
    private static native int pathCreateDirectory(int fd, Address path, int length);

    @Import(module = "wasi_snapshot_preview1", name = "path_unlink_file")
    private static native int pathUnlinkFile(int fd, Address path, int length);

    @Import(module = "wasi_snapshot_preview1", name = "path_remove_directory")
    private static native int pathRemoveDirectory(int fd, Address path, int length);

    @Import(module = "wasi_snapshot_preview1", name = "path_rename")
    private static native int pathRename(int fd, Address from, int fromLength, int toFd,
            Address to, int toLength);
}
