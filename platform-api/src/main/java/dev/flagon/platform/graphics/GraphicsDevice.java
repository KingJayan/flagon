package dev.flagon.platform.graphics;

/**
 * a modern, host-agnostic rendering surface: roughly gl es 3.0, addressed by int names.
 *
 * <p>int names rather than objects because the layer above speaks lwjgl, where every resource
 * is already an int. anything older than this — matrix stacks, immediate mode, fixed-function
 * state — is emulated above, not here.
 */
public interface GraphicsDevice {

    int NO_BUFFER = 0;
    int NO_TEXTURE = 0;
    int NO_PROGRAM = 0;

    void viewport(int x, int y, int width, int height);

    void clearColor(float red, float green, float blue, float alpha);

    void clearDepth(double depth);

    void clear(boolean color, boolean depth, boolean stencil);

    void setDepthTest(boolean enabled);

    void setDepthWrite(boolean enabled);

    void setDepthFunc(CompareFunc func);

    void setBlend(boolean enabled);

    void setBlendFunc(BlendFactor source, BlendFactor destination);

    void setCullFace(boolean enabled);

    void setColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    int createBuffer();

    void deleteBuffer(int buffer);

    void bindArrayBuffer(int buffer);

    void bindElementBuffer(int buffer);

    /** uploads {@code length} floats from {@code data}, replacing the bound buffer's contents. */
    void bufferData(float[] data, int length, BufferUsage usage);

    /** uploads {@code length} indices from {@code data} into the bound element buffer. */
    void elementData(int[] data, int length, BufferUsage usage);

    /**
     * @throws ShaderException if either stage fails to compile, or the program fails to link
     */
    int createProgram(String vertexSource, String fragmentSource);

    void deleteProgram(int program);

    void useProgram(int program);

    /** @return the uniform's location, or -1 when the shader does not use it */
    int uniformLocation(int program, String name);

    void uniformMatrix4(int location, float[] matrix);

    void uniform1i(int location, int value);

    void uniform1f(int location, float value);

    void uniform4f(int location, float x, float y, float z, float w);

    int attributeLocation(int program, String name);

    void enableAttribute(int location);

    void disableAttribute(int location);

    /** offsets and strides are in floats, not bytes; the layer above never sees byte layout. */
    void attributePointer(int location, int components, int strideInFloats, int offsetInFloats);

    int createTexture();

    void deleteTexture(int texture);

    void bindTexture(int unit, int texture);

    /** uploads rgba8 pixels, one int per pixel in 0xAARRGGBB order, top row first. */
    void textureImage(int width, int height, int[] pixels);

    void setTextureFilter(boolean smooth);

    void drawArrays(Primitive primitive, int first, int count);

    /** draws {@code count} indices from the bound element buffer. */
    void drawElements(Primitive primitive, int count);

    int width();

    int height();

    /** @return the first error since the last call, or {@code null} if there was none */
    String takeError();
}
