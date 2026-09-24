package org.lwjgl.util.glu;

import dev.flagon.lwjgl.internal.Matrix4;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class GLU {

    public static final int GLU_INVALID_ENUM = 100900;
    public static final int GLU_INVALID_VALUE = 100901;
    public static final int GLU_OUT_OF_MEMORY = 100902;

    private GLU() {
    }

    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        if (aspect == 0.0f || zNear == zFar) {
            throw new IllegalArgumentException(
                    "gluPerspective: aspect " + aspect + " and a " + zNear + ".." + zFar
                            + " range");
        }
        float top = (float) (zNear * Math.tan(Math.toRadians(fovy / 2.0f)));
        float right = top * aspect;
        GL11.glFrustum(-right, right, -top, top, zNear, zFar);
    }

    public static void gluLookAt(float eyeX, float eyeY, float eyeZ,
                                 float centerX, float centerY, float centerZ,
                                 float upX, float upY, float upZ) {
        float[] forward = normalize(centerX - eyeX, centerY - eyeY, centerZ - eyeZ);
        float[] up = normalize(upX, upY, upZ);
        float[] side = normalize(cross(forward, up));
        float[] trueUp = cross(side, forward);

        float[] view = new float[] {
                side[0], trueUp[0], -forward[0], 0.0f,
                side[1], trueUp[1], -forward[1], 0.0f,
                side[2], trueUp[2], -forward[2], 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
        };
        GL11.glMultMatrixf(view);
        GL11.glTranslatef(-eyeX, -eyeY, -eyeZ);
    }

    public static void gluOrtho2D(float left, float right, float bottom, float top) {
        GL11.glOrtho(left, right, bottom, top, -1.0, 1.0);
    }

    public static void gluPickMatrix(float x, float y, float deltaX, float deltaY,
                                     IntBuffer viewport) {
        if (deltaX <= 0.0f || deltaY <= 0.0f) {
            throw new IllegalArgumentException(
                    "gluPickMatrix: a " + deltaX + "x" + deltaY + " pick box");
        }
        int[] view = viewport(viewport);
        GL11.glTranslatef((view[2] - 2.0f * (x - view[0])) / deltaX,
                (view[3] - 2.0f * (y - view[1])) / deltaY, 0.0f);
        GL11.glScalef(view[2] / deltaX, view[3] / deltaY, 1.0f);
    }

    public static boolean gluProject(float objectX, float objectY, float objectZ,
                                     FloatBuffer modelView, FloatBuffer projection,
                                     IntBuffer viewport, FloatBuffer windowPosition) {
        float[] eye = Matrix4.transform(matrix(modelView, "gluProject"),
                objectX, objectY, objectZ, 1.0f);
        float[] clip = Matrix4.transform(matrix(projection, "gluProject"),
                eye[0], eye[1], eye[2], eye[3]);
        if (clip[3] == 0.0f) {
            return false;
        }
        int[] view = viewport(viewport);
        float x = clip[0] / clip[3];
        float y = clip[1] / clip[3];
        float z = clip[2] / clip[3];
        int at = windowPosition.position();
        windowPosition.put(at, view[0] + view[2] * (x + 1.0f) / 2.0f);
        windowPosition.put(at + 1, view[1] + view[3] * (y + 1.0f) / 2.0f);
        windowPosition.put(at + 2, (z + 1.0f) / 2.0f);
        return true;
    }

    public static boolean gluUnProject(float windowX, float windowY, float windowZ,
                                       FloatBuffer modelView, FloatBuffer projection,
                                       IntBuffer viewport, FloatBuffer objectPosition) {
        float[] combined = Matrix4.copy(matrix(projection, "gluUnProject"));
        Matrix4.multiply(combined, matrix(modelView, "gluUnProject"));
        float[] inverse = Matrix4.invert(combined);
        if (inverse == null) {
            return false;
        }
        int[] view = viewport(viewport);
        float x = 2.0f * (windowX - view[0]) / view[2] - 1.0f;
        float y = 2.0f * (windowY - view[1]) / view[3] - 1.0f;
        float z = 2.0f * windowZ - 1.0f;
        float[] object = Matrix4.transform(inverse, x, y, z, 1.0f);
        if (object[3] == 0.0f) {
            return false;
        }
        int at = objectPosition.position();
        objectPosition.put(at, object[0] / object[3]);
        objectPosition.put(at + 1, object[1] / object[3]);
        objectPosition.put(at + 2, object[2] / object[3]);
        return true;
    }

    public static int gluBuild2DMipmaps(int target, int components, int width, int height,
                                        int format, int type, ByteBuffer data) {
        if (width <= 0 || height <= 0) {
            return GLU_INVALID_VALUE;
        }
        GL11.glTexImage2D(target, 0, components, width, height, 0, format, type, data);
        GL11.glGenerateMipmap(target);
        return 0;
    }

    public static int gluScaleImage(int format, int widthIn, int heightIn, int typeIn,
                                    ByteBuffer dataIn, int widthOut, int heightOut, int typeOut,
                                    ByteBuffer dataOut) {
        if (format != GL11.GL_RGBA || typeIn != GL11.GL_UNSIGNED_BYTE
                || typeOut != GL11.GL_UNSIGNED_BYTE) {
            return GLU_INVALID_ENUM;
        }
        if (widthIn <= 0 || heightIn <= 0 || widthOut <= 0 || heightOut <= 0) {
            return GLU_INVALID_VALUE;
        }
        if (dataIn.remaining() < widthIn * heightIn * 4
                || dataOut.remaining() < widthOut * heightOut * 4) {
            return GLU_OUT_OF_MEMORY;
        }
        int from = dataIn.position();
        int to = dataOut.position();
        for (int row = 0; row < heightOut; row++) {
            int sourceRow = row * heightIn / heightOut;
            for (int column = 0; column < widthOut; column++) {
                int sourceColumn = column * widthIn / widthOut;
                int source = from + (sourceRow * widthIn + sourceColumn) * 4;
                int target = to + (row * widthOut + column) * 4;
                for (int channel = 0; channel < 4; channel++) {
                    dataOut.put(target + channel, dataIn.get(source + channel));
                }
            }
        }
        return 0;
    }

    public static String gluErrorString(int error) {
        return switch (error) {
            case GL11.GL_NO_ERROR -> "no error";
            case GL11.GL_INVALID_ENUM -> "invalid enum";
            case GL11.GL_INVALID_VALUE -> "invalid value";
            case GL11.GL_INVALID_OPERATION -> "invalid operation";
            case GL11.GL_INVALID_FRAMEBUFFER_OPERATION -> "invalid framebuffer operation";
            case GL11.GL_OUT_OF_MEMORY -> "out of memory";
            case GLU_INVALID_ENUM -> "invalid enum";
            case GLU_INVALID_VALUE -> "invalid value";
            case GLU_OUT_OF_MEMORY -> "out of memory";
            default -> "unknown error 0x" + Integer.toHexString(error);
        };
    }

    private static float[] matrix(FloatBuffer buffer, String call) {
        if (buffer.remaining() < 16) {
            throw new IllegalArgumentException(
                    call + " wants a sixteen float matrix, got " + buffer.remaining());
        }
        float[] matrix = new float[16];
        buffer.duplicate().get(matrix);
        return matrix;
    }

    private static int[] viewport(IntBuffer buffer) {
        if (buffer.remaining() < 4) {
            throw new IllegalArgumentException(
                    "a viewport is four integers, got " + buffer.remaining());
        }
        int[] view = new int[4];
        buffer.duplicate().get(view);
        return view;
    }

    private static float[] normalize(float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0f) {
            throw new IllegalArgumentException("a direction of zero length has no direction");
        }
        return new float[] {x / length, y / length, z / length};
    }

    private static float[] normalize(float[] vector) {
        return normalize(vector[0], vector[1], vector[2]);
    }

    private static float[] cross(float[] left, float[] right) {
        return new float[] {
                left[1] * right[2] - left[2] * right[1],
                left[2] * right[0] - left[0] * right[2],
                left[0] * right[1] - left[1] * right[0],
        };
    }
}
