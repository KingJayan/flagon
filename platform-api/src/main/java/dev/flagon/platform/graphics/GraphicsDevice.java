package dev.flagon.platform.graphics;

/**
 a modern, host-agnostic rendering surface: roughly gl es 3.0, addressed by int names
 */
public interface GraphicsDevice {

    int NO_BUFFER = 0;
    int NO_TEXTURE = 0;
    int NO_PROGRAM = 0;
    int NO_SHADER = 0;

    /** a uniform the shader does not use */
    int NO_UNIFORM = -1;

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

    void setCulledFaces(CullFace faces);

    void setFrontFace(Winding winding);

    void setScissorTest(boolean enabled);

    /** the scissor rectangle, in pixels from the framebuffer's bottom left as gl measures it. */
    void setScissor(int x, int y, int width, int height);

    void setColorMask(boolean red, boolean green, boolean blue, boolean alpha);

    void setStencilTest(boolean enabled);

    void setStencilFunc(CompareFunc func, int reference, int mask);

    void setStencilOp(StencilOp stencilFail, StencilOp depthFail, StencilOp pass);

    void setStencilMask(int mask);

    void clearStencil(int value);

    void setPolygonOffsetFill(boolean enabled);

    void setPolygonOffset(float factor, float units);

    void setLineWidth(float width);

    int createBuffer();

    void deleteBuffer(int buffer);

    void bindBuffer(BufferTarget target, int buffer);

    /** reserves {@code sizeInBytes} of uninitialised storage, replacing any contents. */
    void bufferData(BufferTarget target, int sizeInBytes, BufferUsage usage);

    /** uploads {@code length} bytes from {@code data}, replacing the bound buffer's contents. */
    void bufferData(BufferTarget target, byte[] data, int length, BufferUsage usage);

    /** uploads {@code length} floats from {@code data}, replacing the bound buffer's contents. */
    void bufferData(BufferTarget target, float[] data, int length, BufferUsage usage);

    /** uploads {@code length} ints from {@code data}, replacing the bound buffer's contents. */
    void bufferData(BufferTarget target, int[] data, int length, BufferUsage usage);

    /** overwrites {@code length} bytes at {@code offsetInBytes}, leaving the rest in place. */
    void bufferSubData(BufferTarget target, int offsetInBytes, byte[] data, int length);

    int createProgram(String vertexSource, String fragmentSource);

    int createShader(ShaderStage stage);

    /** the source must be glsl es; a host is free to reject anything else. */
    void shaderSource(int shader, String source);

    void compileShader(int shader);

    boolean isShaderCompiled(int shader);

    /** @return the compiler's diagnostics, empty when there were none */
    String shaderInfoLog(int shader);

    void deleteShader(int shader);

    boolean isShader(int shader);

    int createProgram();

    void attachShader(int program, int shader);

    void detachShader(int program, int shader);

    /** binds {@code name} to an attribute location; only takes effect on the next link. */
    void bindAttributeLocation(int program, int location, String name);

    void linkProgram(int program);

    boolean isProgramLinked(int program);

    void validateProgram(int program);

    boolean isProgramValid(int program);

    String programInfoLog(int program);

    boolean isProgram(int program);

    void deleteProgram(int program);

    void useProgram(int program);

    /** @return the uniform's location, or {@link #NO_UNIFORM} when the shader does not use it */
    int uniformLocation(int program, String name);

    void uniformMatrix4(int location, float[] matrix);

    void uniform1i(int location, int value);

    void uniform1f(int location, float value);

    void uniform2f(int location, float x, float y);

    void uniform3f(int location, float x, float y, float z);

    void uniform4f(int location, float x, float y, float z, float w);

    void uniform2i(int location, int x, int y);

    void uniform3i(int location, int x, int y, int z);

    void uniform4i(int location, int x, int y, int z, int w);

    /** sets {@code count} vectors of {@code components} floats each, read from {@code values}. */
    void uniformFloats(int location, int components, float[] values, int count);

    /** sets {@code count} vectors of {@code components} ints each, read from {@code values}. */
    void uniformInts(int location, int components, int[] values, int count);

    /** sets {@code count} square matrices of {@code size}x{@code size} floats, column-major. */
    void uniformMatrices(int location, int size, boolean transpose, float[] values, int count);

    int attributeLocation(int program, String name);

    int maxAttributes();

    void enableAttribute(int location);

    void disableAttribute(int location);

    void attributePointer(int location, int components, AttributeType type, boolean normalized,
                          int strideInBytes, int offsetInBytes);

    void constantAttribute(int location, float x, float y, float z, float w);

    int createTexture();

    void deleteTexture(int texture);

    void bindTexture(int unit, int texture);

    void textureImage(int width, int height, byte[] rgba);

    void textureSubImage(int x, int y, int width, int height, byte[] rgba);

    /** copies a rectangle of the framebuffer into the bound texture, as gl reads it: bottom left up. */
    void copyTextureSubImage(int x, int y, int framebufferX, int framebufferY,
                             int width, int height);

    void setTextureMinFilter(TextureFilter filter);

    void setTextureMagFilter(TextureFilter filter);

    void setTextureWrapS(TextureWrap wrap);

    void setTextureWrapT(TextureWrap wrap);

    void drawArrays(Primitive primitive, int first, int count);

    void drawElements(Primitive primitive, int count, IndexType type, int offsetInBytes);

    /** reads a rectangle of the framebuffer as rgba bytes, the first row being the bottom one. */
    byte[] readPixels(int x, int y, int width, int height);

    void finish();

    void flush();

    int width();

    int height();

    /** @return the first error since the last call, {@link GraphicsError#NONE} when there was none */
    GraphicsError takeError();

    /** @return how the device describes itself, empty when the host will not say */
    String information(GraphicsInfo information);

    int limit(GraphicsLimit limit);
}
