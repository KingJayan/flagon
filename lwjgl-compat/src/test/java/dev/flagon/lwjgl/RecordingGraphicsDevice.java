package dev.flagon.lwjgl;

import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.IndexType;
import dev.flagon.platform.graphics.Primitive;

import java.util.ArrayList;
import java.util.List;

/**
 * records every call instead of drawing; nothing above platform-api needs a browser.
 */
final class RecordingGraphicsDevice implements GraphicsDevice {

    final List<String> calls = new ArrayList<>();
    float[] lastBufferData = new float[0];
    int[] lastElementData = new int[0];
    byte[] lastByteData = new byte[0];
    float[] lastModelView = new float[16];
    float[] lastProjection = new float[16];

    private int nextName = 1;
    private int modelViewLocation = -1;
    private int projectionLocation = -1;

    @Override
    public void viewport(int x, int y, int width, int height) {
        calls.add("viewport " + x + " " + y + " " + width + " " + height);
    }

    @Override
    public void clearColor(float red, float green, float blue, float alpha) {
        calls.add("clearColor " + red + " " + green + " " + blue + " " + alpha);
    }

    @Override
    public void clearDepth(double depth) {
        calls.add("clearDepth " + depth);
    }

    @Override
    public void clear(boolean color, boolean depth, boolean stencil) {
        calls.add("clear " + color + " " + depth + " " + stencil);
    }

    @Override
    public void setDepthTest(boolean enabled) {
        calls.add("depthTest " + enabled);
    }

    @Override
    public void setDepthWrite(boolean enabled) {
        calls.add("depthWrite " + enabled);
    }

    @Override
    public void setDepthFunc(CompareFunc func) {
        calls.add("depthFunc " + func);
    }

    @Override
    public void setBlend(boolean enabled) {
        calls.add("blend " + enabled);
    }

    @Override
    public void setBlendFunc(BlendFactor source, BlendFactor destination) {
        calls.add("blendFunc " + source + " " + destination);
    }

    @Override
    public void setCullFace(boolean enabled) {
        calls.add("cullFace " + enabled);
    }

    @Override
    public void setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        calls.add("colorMask " + red + " " + green + " " + blue + " " + alpha);
    }

    @Override
    public int createBuffer() {
        return nextName++;
    }

    @Override
    public void deleteBuffer(int buffer) {
        calls.add("deleteBuffer " + buffer);
    }

    @Override
    public void bindBuffer(BufferTarget target, int buffer) {
        calls.add("bind" + target + " " + buffer);
    }

    @Override
    public void bufferData(BufferTarget target, int sizeInBytes, BufferUsage usage) {
        calls.add("reserve" + target + " " + sizeInBytes + " " + usage);
    }

    @Override
    public void bufferData(BufferTarget target, byte[] data, int length, BufferUsage usage) {
        lastByteData = new byte[length];
        System.arraycopy(data, 0, lastByteData, 0, length);
        calls.add("byteData" + target + " " + length + " " + usage);
    }

    @Override
    public void bufferData(BufferTarget target, float[] data, int length, BufferUsage usage) {
        lastBufferData = new float[length];
        System.arraycopy(data, 0, lastBufferData, 0, length);
        calls.add("bufferData " + length + " " + usage);
    }

    @Override
    public void bufferData(BufferTarget target, int[] data, int length, BufferUsage usage) {
        lastElementData = new int[length];
        System.arraycopy(data, 0, lastElementData, 0, length);
        calls.add("elementData " + length + " " + usage);
    }

    @Override
    public void bufferSubData(BufferTarget target, int offsetInBytes, byte[] data, int length) {
        lastByteData = new byte[length];
        System.arraycopy(data, 0, lastByteData, 0, length);
        calls.add("byteSubData" + target + " " + offsetInBytes + " " + length);
    }

    @Override
    public int createProgram(String vertexSource, String fragmentSource) {
        return nextName++;
    }

    @Override
    public void deleteProgram(int program) {
        calls.add("deleteProgram " + program);
    }

    @Override
    public void useProgram(int program) {
        calls.add("useProgram " + program);
    }

    @Override
    public int uniformLocation(int program, String name) {
        int location = nextName++;
        if (name.equals("u_modelView")) {
            modelViewLocation = location;
        } else if (name.equals("u_projection")) {
            projectionLocation = location;
        }
        return location;
    }

    @Override
    public void uniformMatrix4(int location, float[] matrix) {
        if (location == modelViewLocation) {
            lastModelView = matrix.clone();
        } else if (location == projectionLocation) {
            lastProjection = matrix.clone();
        }
        calls.add("uniformMatrix4 " + location);
    }

    @Override
    public void uniform1i(int location, int value) {
        calls.add("uniform1i " + location + " " + value);
    }

    @Override
    public void uniform1f(int location, float value) {
        calls.add("uniform1f " + location + " " + value);
    }

    @Override
    public void uniform4f(int location, float x, float y, float z, float w) {
        calls.add("uniform4f " + location);
    }

    @Override
    public int attributeLocation(int program, String name) {
        return nextName++;
    }

    @Override
    public void enableAttribute(int location) {
        calls.add("enableAttribute " + location);
    }

    @Override
    public void disableAttribute(int location) {
        calls.add("disableAttribute " + location);
    }

    @Override
    public void attributePointer(int location, int components, AttributeType type,
                                 boolean normalized, int strideInBytes, int offsetInBytes) {
        calls.add("attributePointer " + location + " " + components + " " + type + " "
                + normalized + " " + strideInBytes + " " + offsetInBytes);
    }

    @Override
    public void constantAttribute(int location, float x, float y, float z, float w) {
        calls.add("constantAttribute " + location + " " + x + " " + y + " " + z + " " + w);
    }

    @Override
    public int createTexture() {
        return nextName++;
    }

    @Override
    public void deleteTexture(int texture) {
        calls.add("deleteTexture " + texture);
    }

    @Override
    public void bindTexture(int unit, int texture) {
        calls.add("bindTexture " + unit + " " + texture);
    }

    @Override
    public void textureImage(int width, int height, int[] pixels) {
        calls.add("textureImage " + width + "x" + height);
    }

    @Override
    public void setTextureFilter(boolean smooth) {
        calls.add("textureFilter " + smooth);
    }

    @Override
    public void drawArrays(Primitive primitive, int first, int count) {
        calls.add("drawArrays " + primitive + " " + first + " " + count);
    }

    @Override
    public void drawElements(Primitive primitive, int count, IndexType type, int offsetInBytes) {
        calls.add("drawElements " + primitive + " " + count + " " + type + " " + offsetInBytes);
    }

    @Override
    public int width() {
        return 854;
    }

    @Override
    public int height() {
        return 480;
    }

    @Override
    public String takeError() {
        return null;
    }

    /** @return the recorded calls whose first word matches {@code name}. */
    List<String> calls(String name) {
        return calls.stream().filter(call -> call.startsWith(name + " ")).toList();
    }
}
