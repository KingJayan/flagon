package dev.flagon.platform.browser.js;

import org.teavm.interop.PlatformMarker;
import org.teavm.interop.Platforms;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.typedarrays.Uint32Array;
import org.teavm.jso.JSBody;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * hands the first {@code length} elements of a java array to a browser api as a typed array
 *
 * <p>compiled to javascript a java array is already a typed array, so the browser is given a view
 * over the array's own storage and nothing is copied. compiled to wasm gc it lives in the wasm heap
 * where javascript cannot see it, so exactly the elements being passed are copied out — the
 * reason
 * callers say how many they mean rather than passing a whole growable array.
 *
 * <p>the same boundary runs the other way, where the only thing that matters is that it is crossed
 * once for a whole array rather than once for each of its elements.
 */
public final class JsArrays {

    private JsArrays() {
    }

    /** @return the whole of a typed array as a java array, in one crossing */
    public static byte[] bytesOf(Int8Array values) {
        return values.copyToJavaArray();
    }

    public static Int8Array bytes(byte[] values, int length) {
        require(length, values.length);
        if (copies()) {
            return Int8Array.copyFromJavaArray(prefix(values, length));
        }
        Int8Array whole = Int8Array.fromJavaArray(values);
        return Int8Array.create(whole.getBuffer(), whole.getByteOffset(), length);
    }

    public static Float32Array floats(float[] values, int length) {
        require(length, values.length);
        if (copies()) {
            if (length > STORED) {
                return Float32Array.copyFromJavaArray(prefix(values, length));
            }
            if (floatStore == null || floatStore.capacity() < length) {
                floatStore = direct(length * Float.BYTES).asFloatBuffer();
            }
            floatStore.position(0);
            floatStore.put(values, 0, length);
            Float32Array store = Float32Array.fromJavaBuffer(floatStore);
            return new Float32Array(store.getBuffer(), store.getByteOffset(), length);
        }
        Float32Array whole = Float32Array.fromJavaArray(values);
        return Float32Array.create(whole.getBuffer(), whole.getByteOffset(), length);
    }

    /** the same bytes as {@link #bytes}, typed as the pixel apis want them. */
    public static Uint8Array unsignedBytes(byte[] values, int length) {
        require(length, values.length);
        if (copies()) {
            return Uint8Array.create(Int8Array.copyFromJavaArray(prefix(values, length))
                    .getBuffer());
        }
        Int8Array whole = Int8Array.fromJavaArray(values);
        return Uint8Array.create(whole.getBuffer(), whole.getByteOffset(), length);
    }

    public static Int32Array ints(int[] values, int length) {
        require(length, values.length);
        if (copies()) {
            if (length > STORED) {
                return Int32Array.copyFromJavaArray(prefix(values, length));
            }
            if (intStore == null || intStore.capacity() < length) {
                intStore = direct(length * Integer.BYTES).asIntBuffer();
            }
            intStore.position(0);
            intStore.put(values, 0, length);
            Int32Array store = Int32Array.fromJavaBuffer(intStore);
            return new Int32Array(store.getBuffer(), store.getByteOffset(), length);
        }
        Int32Array whole = Int32Array.fromJavaArray(values);
        return Int32Array.create(whole.getBuffer(), whole.getByteOffset(), length);
    }

    public static Uint32Array unsignedInts(int[] values, int length) {
        require(length, values.length);
        if (copies()) {
            return Uint32Array.copyFromJavaArray(prefix(values, length));
        }
        Uint32Array whole = Uint32Array.fromJavaArray(values);
        return new Uint32Array(whole.getBuffer(), whole.getByteOffset(), length);
    }

    /**
     * @param into a store the caller keeps
     * @return the pixels as r, g, b, a bytes
     */
    @JSBody(params = {"argb", "count", "into"}, script = """
            for (var at = 0; at < count; at++) {
                var pixel = argb[at];
                into[at] = (pixel & 0xFF00FF00) | ((pixel & 0x00FF0000) >>> 16)
                        | ((pixel & 0x000000FF) << 16);
            }
            return new Uint8Array(into.buffer, into.byteOffset, count * 4);
            """)
    public static native Uint8Array rgbaOf(Uint32Array argb, int count, Uint32Array into);

    /** biggest upload one store serves */
    private static final int STORED = 64 * 1024;

    // javascript reads a direct buffer where it stands
    private static FloatBuffer floatStore;
    private static IntBuffer intStore;

    private static ByteBuffer direct(int bytes) {
        return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder());
    }

    private static byte[] prefix(byte[] values, int length) {
        if (length == values.length) {
            return values;
        }
        byte[] cut = new byte[length];
        System.arraycopy(values, 0, cut, 0, length);
        return cut;
    }

    private static float[] prefix(float[] values, int length) {
        if (length == values.length) {
            return values;
        }
        float[] cut = new float[length];
        System.arraycopy(values, 0, cut, 0, length);
        return cut;
    }

    private static int[] prefix(int[] values, int length) {
        if (length == values.length) {
            return values;
        }
        int[] cut = new int[length];
        System.arraycopy(values, 0, cut, 0, length);
        return cut;
    }

    private static void require(int length, int available) {
        if (length > available) {
            throw new IllegalArgumentException("length " + length + " exceeds array " + available);
        }
    }

    // folded to a constant at compile time, so only one of the two paths above is ever emitted
    @PlatformMarker(Platforms.WEBASSEMBLY_GC)
    private static boolean copies() {
        return false;
    }
}
