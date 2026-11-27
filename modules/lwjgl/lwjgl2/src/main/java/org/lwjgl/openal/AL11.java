package org.lwjgl.openal;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/** openal 1.1 in both forms: lwjgl 3 extends AL10, and lwjgl 2 names the buffer forms alSource */
public class AL11 extends AL10 {

    public static final int AL_SEC_OFFSET = 0x1024;
    public static final int AL_SAMPLE_OFFSET = 0x1025;
    public static final int AL_BYTE_OFFSET = 0x1026;
    public static final int AL_STATIC = 0x1028;
    public static final int AL_STREAMING = 0x1029;
    public static final int AL_UNDETERMINED = 0x1030;
    public static final int AL_ILLEGAL_COMMAND = 0xA004;

    protected AL11() {
    }

    public static void alSource3i(int source, int parameter, int x, int y, int z) {
        alSource3f(source, parameter, x, y, z);
    }

    public static void alSourceiv(int source, int parameter, IntBuffer values) {
        if (vectorSize(parameter) == 1) {
            alSourcei(source, parameter, values.get(values.position()));
            return;
        }
        float[] vector = floats(values, 3);
        if (vector != null) {
            alSource3f(source, parameter, vector[0], vector[1], vector[2]);
        }
    }

    public static void alSourceiv(int source, int parameter, int[] values) {
        alSourceiv(source, parameter, IntBuffer.wrap(values));
    }

    public static void alSource(int source, int parameter, IntBuffer values) {
        alSourceiv(source, parameter, values);
    }

    public static void alListener3i(int parameter, int x, int y, int z) {
        alListener3f(parameter, x, y, z);
    }

    public static void alListeneriv(int parameter, IntBuffer values) {
        if (parameter != AL_POSITION && parameter != AL_VELOCITY && parameter != AL_ORIENTATION) {
            state().fail(AL_INVALID_ENUM);
            return;
        }
        float[] vector = floats(values, parameter == AL_ORIENTATION ? 6 : 3);
        if (vector != null) {
            alListener(parameter, FloatBuffer.wrap(vector));
        }
    }

    public static void alListeneriv(int parameter, int[] values) {
        alListeneriv(parameter, IntBuffer.wrap(values));
    }

    public static void alGetListeneriv(int parameter, IntBuffer values) {
        float[] vector = listenerVector(parameter);
        if (vector != null) {
            write(values, vector);
        }
    }

    public static void alGetListeneriv(int parameter, int[] values) {
        alGetListeneriv(parameter, IntBuffer.wrap(values));
    }

    /** lwjgl 2 names alGetListenerfv this way */
    public static void alGetListeneri(int parameter, FloatBuffer values) {
        alGetListenerfv(parameter, values);
    }

    // openal 1.1 has no buffer property to set

    public static void alBufferf(int buffer, int parameter, float value) {
        refuseBufferProperty(buffer);
    }

    public static void alBuffer3f(int buffer, int parameter, float x, float y, float z) {
        refuseBufferProperty(buffer);
    }

    public static void alBufferfv(int buffer, int parameter, FloatBuffer values) {
        refuseBufferProperty(buffer);
    }

    public static void alBufferfv(int buffer, int parameter, float[] values) {
        refuseBufferProperty(buffer);
    }

    public static void alBuffer(int buffer, int parameter, FloatBuffer values) {
        refuseBufferProperty(buffer);
    }

    public static void alBufferi(int buffer, int parameter, int value) {
        refuseBufferProperty(buffer);
    }

    public static void alBuffer3i(int buffer, int parameter, int x, int y, int z) {
        refuseBufferProperty(buffer);
    }

    public static void alBufferiv(int buffer, int parameter, IntBuffer values) {
        refuseBufferProperty(buffer);
    }

    public static void alBufferiv(int buffer, int parameter, int[] values) {
        refuseBufferProperty(buffer);
    }

    public static void alBuffer(int buffer, int parameter, IntBuffer values) {
        refuseBufferProperty(buffer);
    }

    public static void alGetBufferiv(int buffer, int parameter, IntBuffer values) {
        alGetBufferi(buffer, parameter, values);
    }

    public static void alGetBufferiv(int buffer, int parameter, int[] values) {
        alGetBufferi(buffer, parameter, values);
    }

    public static void alGetBuffer(int buffer, int parameter, IntBuffer values) {
        alGetBufferi(buffer, parameter, values);
    }

    public static void alGetBufferfv(int buffer, int parameter, FloatBuffer values) {
        alGetBufferf(buffer, parameter, values);
    }

    public static void alGetBufferfv(int buffer, int parameter, float[] values) {
        alGetBufferf(buffer, parameter, values);
    }

    public static void alGetBuffer(int buffer, int parameter, FloatBuffer values) {
        alGetBufferf(buffer, parameter, values);
    }

    private static void refuseBufferProperty(int buffer) {
        state().fail(device().isBuffer(buffer) ? AL_INVALID_ENUM : AL_INVALID_NAME);
    }

    /** @return null once the error is latched */
    private static float[] floats(IntBuffer values, int count) {
        if (values == null || values.remaining() < count) {
            state().fail(AL_INVALID_VALUE);
            return null;
        }
        float[] read = new float[count];
        for (int at = 0; at < count; at++) {
            read[at] = values.get(values.position() + at);
        }
        return read;
    }
}
