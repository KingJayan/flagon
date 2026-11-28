package org.lwjgl.opengles;

import dev.flagon.lwjgl.internal.Buffers;
import dev.flagon.lwjgl.internal.GLState;
import dev.flagon.platform.graphics.EsContext;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

/** what gl es asks of client memory, done over a context that only takes arrays. */
final class Es {

    static final int PIXEL_PACK_BUFFER_BINDING = 0x88ED;
    static final int PIXEL_UNPACK_BUFFER_BINDING = 0x88EF;

    private static final int ARRAY_BUFFER = 0x8892;
    private static final int ELEMENT_ARRAY_BUFFER = 0x8893;
    private static final int ARRAY_BUFFER_BINDING = 0x8894;
    private static final int ELEMENT_ARRAY_BUFFER_BINDING = 0x8895;
    private static final int STREAM_DRAW = 0x88E0;
    private static final int MAP_READ_BIT = 0x0001;
    private static final int MAP_WRITE_BIT = 0x0002;
    private static final int MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    private static final int TIMEOUT_EXPIRED = 0x911B;
    private static final int QUERY_RESULT = 0x8866;
    private static final int QUERY_RESULT_AVAILABLE = 0x8867;
    private static final long QUERY_PATIENCE_MS = 5_000;

    /** an attribute that reads client memory, uploaded again before each draw */
    private record ClientArray(Buffer data, int size, int type, boolean normalized, int stride,
                               boolean integer) {
    }

    private record Mapping(ByteBuffer data, long offset, int access) {
    }

    private static final Map<Integer, ClientArray> CLIENT_ARRAYS = new HashMap<>();
    private static final Map<Integer, Integer> CLIENT_BUFFERS = new HashMap<>();
    private static final Map<Integer, Mapping> MAPPINGS = new HashMap<>();
    private static int clientIndices;
    private static int pendingError;

    private Es() {
    }

    static EsContext context() {
        return GLState.get().device().es();
    }

    /** records an error gl es raises where webgl has no call to raise it */
    static void fail(int error) {
        if (pendingError == 0) {
            pendingError = error;
        }
    }

    static int error() {
        int error = pendingError;
        pendingError = 0;
        return error != 0 ? error : context().getError();
    }

    /** whether a long pixel argument is an offset into a bound buffer, not an address */
    static boolean bound(int binding) {
        return context().getParameter(binding)[0] != 0;
    }

    /** the bytes width x height x depth pixels take, tightly packed */
    static int pixelBytes(int format, int type, int width, int height, int depth) {
        int pixel = switch (type) {
            case 0x8363, 0x8033, 0x8034 -> 2;
            case 0x8368, 0x8C3B, 0x8C3E, 0x84FA -> 4;
            case 0x8DAD -> 8;
            default -> typeSize(type) * components(format);
        };
        return pixel * width * height * depth;
    }

    private static int components(int format) {
        return switch (format) {
            case 0x8227, 0x8228, 0x190A, 0x84F9 -> 2;
            case 0x1907, 0x8D98 -> 3;
            case 0x1908, 0x8D99 -> 4;
            default -> 1;
        };
    }

    static byte[] bytes(Buffer data) {
        return data == null ? null : Buffers.readRemaining(data);
    }

    /** an array overload's argument as the buffer its buffer overload takes */
    static ShortBuffer wrap(short[] values) {
        return values == null ? null : ShortBuffer.wrap(values);
    }

    static IntBuffer wrap(int[] values) {
        return values == null ? null : IntBuffer.wrap(values);
    }

    static FloatBuffer wrap(float[] values) {
        return values == null ? null : FloatBuffer.wrap(values);
    }

    static LongBuffer wrap(long[] values) {
        return values == null ? null : LongBuffer.wrap(values);
    }

    static float[] floats(FloatBuffer values) {
        float[] read = new float[values.remaining()];
        values.duplicate().get(read);
        return read;
    }

    static int[] ints(IntBuffer values) {
        int[] read = new int[values.remaining()];
        values.duplicate().get(read);
        return read;
    }

    /** writes native-order bytes over the buffer's remaining elements, leaving its position */
    static void write(Buffer target, byte[] bytes) {
        ByteBuffer source = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        int at = target.position();
        if (target instanceof ByteBuffer into) {
            into.duplicate().put(source);
        } else if (target instanceof ShortBuffer into) {
            ShortBuffer view = into.duplicate();
            view.position(at);
            view.put(source.asShortBuffer());
        } else if (target instanceof IntBuffer into) {
            IntBuffer view = into.duplicate();
            view.position(at);
            view.put(source.asIntBuffer());
        } else if (target instanceof FloatBuffer into) {
            FloatBuffer view = into.duplicate();
            view.position(at);
            view.put(source.asFloatBuffer());
        } else {
            throw new IllegalArgumentException("unsupported buffer " + target.getClass());
        }
    }

    static void put(IntBuffer into, int[] values) {
        for (int i = 0; i < values.length && i < into.remaining(); i++) {
            into.put(into.position() + i, values[i]);
        }
    }

    static void put(IntBuffer into, double[] values) {
        for (int i = 0; i < values.length && i < into.remaining(); i++) {
            into.put(into.position() + i, (int) (long) values[i]);
        }
    }

    static void put(FloatBuffer into, double[] values) {
        for (int i = 0; i < values.length && i < into.remaining(); i++) {
            into.put(into.position() + i, (float) values[i]);
        }
    }

    static void put(ByteBuffer into, double[] values) {
        for (int i = 0; i < values.length && i < into.remaining(); i++) {
            into.put(into.position() + i, (byte) (values[i] != 0 ? 1 : 0));
        }
    }

    static void put(LongBuffer into, long value) {
        into.put(into.position(), value);
    }

    static void putIfPresent(IntBuffer into, int value) {
        if (into != null) {
            into.put(into.position(), value);
        }
    }

    /** a nul-terminated name, as lwjgl encodes one */
    static String text(ByteBuffer encoded) {
        int end = encoded.position();
        while (end < encoded.limit() && encoded.get(end) != 0) {
            end++;
        }
        byte[] bytes = new byte[end - encoded.position()];
        encoded.duplicate().get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static String text(PointerBuffer strings, int index, IntBuffer lengths) {
        long address = strings.get(strings.position() + index);
        int length = lengths == null ? -1 : lengths.get(lengths.position() + index);
        return length < 0 ? MemoryUtil.memUTF8(address)
                : MemoryUtil.memUTF8(MemoryUtil.memByteBuffer(address, length));
    }

    static String[] texts(PointerBuffer strings) {
        String[] read = new String[strings.remaining()];
        for (int i = 0; i < read.length; i++) {
            read[i] = text(strings, i, null);
        }
        return read;
    }

    /** copies text the way gl does: as much as fits with a nul, and the length without it */
    static void write(String text, IntBuffer length, ByteBuffer into) {
        byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
        int count = Math.max(0, Math.min(bytes.length, into.remaining() - 1));
        ByteBuffer view = into.duplicate();
        view.put(bytes, 0, count);
        if (into.hasRemaining()) {
            view.put((byte) 0);
        }
        putIfPresent(length, count);
    }

    static String clip(String text, int maxLength) {
        String whole = text == null ? "" : text;
        return maxLength <= 0 ? "" : whole.length() < maxLength ? whole
                : whole.substring(0, maxLength - 1);
    }

    static int[] names(IntBuffer into, java.util.function.IntSupplier create) {
        int[] made = new int[into.remaining()];
        for (int i = 0; i < made.length; i++) {
            made[i] = create.getAsInt();
            into.put(into.position() + i, made[i]);
        }
        return made;
    }

    static void delete(IntBuffer names, java.util.function.IntConsumer delete) {
        for (int i = names.position(); i < names.limit(); i++) {
            delete.accept(names.get(i));
        }
    }

    // client arrays

    static void pointAt(int index, Buffer data, int size, int type, boolean normalized,
                        int stride, boolean integer) {
        CLIENT_ARRAYS.put(index, new ClientArray(data, size, type, normalized, stride, integer));
    }

    static void pointAt(int index) {
        CLIENT_ARRAYS.remove(index);
    }

    static Buffer clientPointer(int index) {
        ClientArray array = CLIENT_ARRAYS.get(index);
        return array == null ? null : array.data();
    }

    /** uploads the client arrays a draw is about to read */
    static EsContext drawing() {
        EsContext context = context();
        if (CLIENT_ARRAYS.isEmpty()) {
            return context;
        }
        int bound = (int) context.getParameter(ARRAY_BUFFER_BINDING)[0];
        for (Map.Entry<Integer, ClientArray> entry : CLIENT_ARRAYS.entrySet()) {
            int index = entry.getKey();
            ClientArray array = entry.getValue();
            int buffer = CLIENT_BUFFERS.computeIfAbsent(index, ignored -> context.createBuffer());
            context.bindBuffer(ARRAY_BUFFER, buffer);
            context.bufferData(ARRAY_BUFFER, bytes(array.data()), STREAM_DRAW);
            if (array.integer()) {
                context.vertexAttribIPointer(index, array.size(), array.type(), array.stride(), 0);
            } else {
                context.vertexAttribPointer(index, array.size(), array.type(),
                        array.normalized(), array.stride(), 0);
            }
        }
        context.bindBuffer(ARRAY_BUFFER, bound);
        return context;
    }

    /** draws indices from client memory through a scratch element buffer */
    static void drawElements(int mode, int type, Buffer indices, int start, int end,
                             int instances) {
        EsContext context = drawing();
        int bound = (int) context.getParameter(ELEMENT_ARRAY_BUFFER_BINDING)[0];
        if (clientIndices == 0) {
            clientIndices = context.createBuffer();
        }
        byte[] bytes = bytes(indices);
        int count = bytes.length / typeSize(type);
        context.bindBuffer(ELEMENT_ARRAY_BUFFER, clientIndices);
        context.bufferData(ELEMENT_ARRAY_BUFFER, bytes, STREAM_DRAW);
        if (instances >= 0) {
            context.drawElementsInstanced(mode, count, type, 0, instances);
        } else if (end >= start && start >= 0) {
            context.drawRangeElements(mode, start, end, count, type, 0);
        } else {
            context.drawElements(mode, count, type, 0);
        }
        context.bindBuffer(ELEMENT_ARRAY_BUFFER, bound);
    }

    static int typeSize(int type) {
        return switch (type) {
            case 0x1400, 0x1401 -> 1;
            case 0x1402, 0x1403, 0x140B, 0x8D61 -> 2;
            default -> 4;
        };
    }

    // mapped buffers

    static ByteBuffer map(int target, long offset, long length, int access) {
        ByteBuffer data = ByteBuffer.allocateDirect((int) length).order(ByteOrder.nativeOrder());
        if ((access & MAP_READ_BIT) != 0) {
            byte[] read = new byte[(int) length];
            context().getBufferSubData(target, offset, read);
            data.put(read).clear();
        }
        MAPPINGS.put(target, new Mapping(data, offset, access));
        return data;
    }

    static ByteBuffer mapped(int target) {
        Mapping mapping = MAPPINGS.get(target);
        return mapping == null ? null : mapping.data();
    }

    static void flushMapped(int target, long offset, long length) {
        Mapping mapping = MAPPINGS.get(target);
        if (mapping != null) {
            upload(target, mapping, (int) offset, (int) length);
        }
    }

    static boolean unmap(int target) {
        Mapping mapping = MAPPINGS.remove(target);
        if (mapping == null) {
            return false;
        }
        if ((mapping.access() & MAP_WRITE_BIT) != 0
                && (mapping.access() & MAP_FLUSH_EXPLICIT_BIT) == 0) {
            upload(target, mapping, 0, mapping.data().capacity());
        }
        return true;
    }

    private static void upload(int target, Mapping mapping, int from, int length) {
        byte[] bytes = new byte[length];
        mapping.data().duplicate().clear().position(from).get(bytes);
        context().bufferSubData(target, mapping.offset() + from, bytes);
    }

    /** a mapped buffer's state, or -1 when the target is not mapped */
    static long mappedParameter(int target, int name) {
        Mapping mapping = MAPPINGS.get(target);
        return switch (name) {
            case 0x88BC -> mapping == null ? 0 : 1;
            case 0x911F -> mapping == null ? 0 : mapping.access();
            case 0x9120 -> mapping == null ? 0 : mapping.data().capacity();
            case 0x9121 -> mapping == null ? 0 : mapping.offset();
            default -> -1;
        };
    }

    // waiting

    static int clientWaitSync(int sync, int flags, long timeout) {
        EsContext context = context();
        long deadline = System.nanoTime() + Math.max(0, timeout);
        while (true) {
            int status = context.clientWaitSync(sync, flags, 0);
            if (status != TIMEOUT_EXPIRED || System.nanoTime() >= deadline) {
                return status;
            }
            context.flush();
            pause();
        }
    }

    static long queryResult(int query, int name) {
        EsContext context = context();
        if (name == QUERY_RESULT) {
            long deadline = System.currentTimeMillis() + QUERY_PATIENCE_MS;
            while (context.getQueryParameter(query, QUERY_RESULT_AVAILABLE) == 0
                    && System.currentTimeMillis() < deadline) {
                pause();
            }
        }
        return context.getQueryParameter(query, name);
    }

    /** gives the page its turn, which is when webgl answers queries and fences */
    private static void pause() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException stopped) {
            Thread.currentThread().interrupt();
        }
    }
}
