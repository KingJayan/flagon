package org.teavm.runtime.wasi;

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.runtime.Fiber;
import org.teavm.runtime.heap.Heap;

/**
 * flagon: what a webassembly gc bundle calls in place of javascript when it runs under wasi.
 * every call is to wasi preview 1, which an adapter turns into preview 2 for a component.
 */
public final class Wasi {
    public static final int STDOUT = 1;
    public static final int STDERR = 2;
    private static final int CLOCK_REALTIME = 0;
    private static final int CLOCK_MONOTONIC = 1;
    private static final int SCRATCH = 64;

    private static Address scratch;
    private static byte[] outBuffer = new byte[256];
    private static int outLength;
    private static byte[] errBuffer = new byte[256];
    private static int errLength;

    private Wasi() {
    }

    /** what the module's _start export runs: main on a fiber, then the event queue until done */
    public static void run() {
        try {
            Fiber.runToEnd(arguments());
        } catch (Throwable uncaught) {
            uncaught.printStackTrace();
            exit(1);
        }
        flush(STDOUT);
        flush(STDERR);
    }

    public static void heapResized() {
    }

    /** a wasi build parks fibers by unwinding them, never on a stack of their own */
    public static void noFiberStack() {
        throw new IllegalStateException("a wasi build has no fiber stacks");
    }

    public static void noFiberStack(int fiber) {
        noFiberStack();
    }

    public static void nothing() {
    }

    /** a few bytes of linear memory for the arguments and results of one call */
    public static Address scratch() {
        if (scratch == null) {
            scratch = Heap.alloc(SCRATCH);
        }
        return scratch;
    }

    public static void putchar(int fd, char c) {
        if (fd == STDERR) {
            if (errLength == errBuffer.length) {
                flush(STDERR);
            }
            errBuffer[errLength++] = (byte) c;
            if (c == '\n') {
                flush(STDERR);
            }
        } else {
            if (outLength == outBuffer.length) {
                flush(STDOUT);
            }
            outBuffer[outLength++] = (byte) c;
            if (c == '\n') {
                flush(STDOUT);
            }
        }
    }

    public static void putCharStdout(char c) {
        putchar(STDOUT, c);
    }

    public static void putCharStderr(char c) {
        putchar(STDERR, c);
    }

    public static void flush(int fd) {
        if (fd == STDERR) {
            write(STDERR, errBuffer, 0, errLength);
            errLength = 0;
        } else {
            write(STDOUT, outBuffer, 0, outLength);
            outLength = 0;
        }
    }

    /** @return the bytes written, or a negative wasi errno */
    public static int write(int fd, byte[] bytes, int offset, int length) {
        if (length == 0) {
            return 0;
        }
        Address data = Heap.alloc(length);
        for (int i = 0; i < length; i++) {
            data.add(i).putByte(bytes[offset + i]);
        }
        int done = 0;
        int errno = 0;
        while (done < length) {
            Address io = scratch();
            io.putInt(data.add(done).toInt());
            io.add(4).putInt(length - done);
            errno = fdWrite(fd, io, 1, io.add(8));
            if (errno != 0) {
                break;
            }
            done += io.add(8).getInt();
        }
        Heap.release(data);
        return errno != 0 ? -errno : done;
    }

    /** @return the bytes read, 0 at the end, or a negative wasi errno */
    public static int read(int fd, byte[] bytes, int offset, int length) {
        if (length == 0) {
            return 0;
        }
        Address data = Heap.alloc(length);
        Address io = scratch();
        io.putInt(data.toInt());
        io.add(4).putInt(length);
        int errno = fdRead(fd, io, 1, io.add(8));
        int count = errno == 0 ? io.add(8).getInt() : -errno;
        for (int i = 0; i < count; i++) {
            bytes[offset + i] = data.add(i).getByte();
        }
        Heap.release(data);
        return count;
    }

    public static String[] arguments() {
        Address sizes = scratch();
        if (argsSizesGet(sizes, sizes.add(4)) != 0) {
            return new String[0];
        }
        int count = sizes.getInt();
        int size = sizes.add(4).getInt();
        Address pointers = Heap.alloc(Math.max(count, 1) * 4);
        Address text = Heap.alloc(Math.max(size, 1));
        String[] result = new String[Math.max(count - 1, 0)];
        if (argsGet(pointers, text) == 0) {
            // the first argument names the program, as a shell does; java's main never sees it
            for (int i = 1; i < count; i++) {
                result[i - 1] = utf8(Address.fromInt(pointers.add(i * 4).getInt()));
            }
        }
        Heap.release(pointers);
        Heap.release(text);
        return result;
    }

    private static String utf8(Address start) {
        int length = 0;
        while (start.add(length).getByte() != 0) {
            length++;
        }
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = start.add(i).getByte();
        }
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static double currentTimeMillis() {
        return time(CLOCK_REALTIME) / 1_000_000L;
    }

    public static long nanoTime() {
        return time(CLOCK_MONOTONIC);
    }

    private static long time(int clock) {
        Address result = scratch();
        clockTimeGet(clock, 1, result);
        return result.getLong();
    }

    /** sleeps the whole module: under wasi nothing else can run while every fiber waits */
    public static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        Address subscription = Heap.alloc(48);
        for (int i = 0; i < 48; i += 8) {
            subscription.add(i).putLong(0);
        }
        // a relative timer on the monotonic clock
        subscription.add(8).putByte((byte) 0);
        subscription.add(16).putInt(CLOCK_MONOTONIC);
        subscription.add(24).putLong(millis * 1_000_000L);
        Address event = Heap.alloc(32);
        pollOneoff(subscription, event, 1, scratch());
        Heap.release(subscription);
        Heap.release(event);
    }

    public static double random() {
        Address bytes = scratch();
        randomGet(bytes, 8);
        return (bytes.getLong() >>> 11) * 0x1.0p-53;
    }

    public static void exit(int code) {
        flush(STDOUT);
        flush(STDERR);
        procExit(code);
    }

    @Import(module = "wasi_snapshot_preview1", name = "fd_write")
    private static native int fdWrite(int fd, Address iovs, int count, Address written);

    @Import(module = "wasi_snapshot_preview1", name = "fd_read")
    private static native int fdRead(int fd, Address iovs, int count, Address read);

    @Import(module = "wasi_snapshot_preview1", name = "args_sizes_get")
    private static native int argsSizesGet(Address count, Address size);

    @Import(module = "wasi_snapshot_preview1", name = "args_get")
    private static native int argsGet(Address pointers, Address text);

    @Import(module = "wasi_snapshot_preview1", name = "clock_time_get")
    private static native int clockTimeGet(int clock, long precision, Address time);

    @Import(module = "wasi_snapshot_preview1", name = "poll_oneoff")
    private static native int pollOneoff(Address in, Address out, int count, Address events);

    @Import(module = "wasi_snapshot_preview1", name = "random_get")
    private static native int randomGet(Address buffer, int length);

    @Import(module = "wasi_snapshot_preview1", name = "proc_exit")
    private static native void procExit(int code);
}
