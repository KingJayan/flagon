package org.lwjgl.util.glu;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * the projection half of glu, which lwjgl 2 also exposes under its own name
 */
public final class Project {

    private Project() {
    }

    public static void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        GLU.gluPerspective(fovy, aspect, zNear, zFar);
    }

    public static void gluOrtho2D(float left, float right, float bottom, float top) {
        GLU.gluOrtho2D(left, right, bottom, top);
    }

    public static void gluLookAt(float eyeX, float eyeY, float eyeZ,
                                 float centerX, float centerY, float centerZ,
                                 float upX, float upY, float upZ) {
        GLU.gluLookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    public static void gluPickMatrix(float x, float y, float deltaX, float deltaY,
                                     IntBuffer viewport) {
        GLU.gluPickMatrix(x, y, deltaX, deltaY, viewport);
    }

    public static boolean gluProject(float objectX, float objectY, float objectZ,
                                     FloatBuffer modelView, FloatBuffer projection,
                                     IntBuffer viewport, FloatBuffer windowPosition) {
        return GLU.gluProject(objectX, objectY, objectZ, modelView, projection, viewport,
                windowPosition);
    }

    public static boolean gluUnProject(float windowX, float windowY, float windowZ,
                                       FloatBuffer modelView, FloatBuffer projection,
                                       IntBuffer viewport, FloatBuffer objectPosition) {
        return GLU.gluUnProject(windowX, windowY, windowZ, modelView, projection, viewport,
                objectPosition);
    }
}
