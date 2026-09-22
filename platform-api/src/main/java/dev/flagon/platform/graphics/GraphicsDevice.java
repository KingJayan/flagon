package dev.flagon.platform.graphics;

/**
host-agnostic rendering surface: roughly gl es 3.0
 */
public interface GraphicsDevice {

    int NO_BUFFER = 0;
    int NO_VERTEX_ARRAY = 0;
    int NO_TEXTURE = 0;
    int NO_PROGRAM = 0;
    int NO_FRAMEBUFFER = 0;
    int NO_RENDERBUFFER = 0;
    int NO_SHADER = 0;

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

    /** color and alpha blend */
    void setBlendFuncSeparate(BlendFactor sourceColor, BlendFactor destinationColor,
                              BlendFactor sourceAlpha, BlendFactor destinationAlpha);

    void setBlendEquation(BlendEquation equation);

    void setBlendEquationSeparate(BlendEquation color, BlendEquation alpha);

    void setBlendColor(float red, float green, float blue, float alpha);

    void setCullFace(boolean enabled);

    void setCulledFaces(CullFace faces);

    void setFrontFace(Winding winding);

    void setScissorTest(boolean enabled);

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

    void bufferSubData(BufferTarget target, int offsetInBytes, byte[] data, int length);

    void bufferSubData(BufferTarget target, int offsetInBytes, float[] data, int length);

    int createProgram(String vertexSource, String fragmentSource);

    int createShader(ShaderStage stage);

    void shaderSource(int shader, String source);

    void compileShader(int shader);

    boolean isShaderCompiled(int shader);

    String shaderInfoLog(int shader);

    void deleteShader(int shader);

    boolean isShader(int shader);

    int createProgram();

    void attachShader(int program, int shader);

    void detachShader(int program, int shader);

    void bindAttributeLocation(int program, int location, String name);

    void linkProgram(int program);

    boolean isProgramLinked(int program);

    void validateProgram(int program);

    boolean isProgramValid(int program);

    String programInfoLog(int program);

    boolean isProgram(int program);

    void deleteProgram(int program);

    void useProgram(int program);

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


    int createVertexArray();

    void bindVertexArray(int array);

    void deleteVertexArray(int array);

    void enableAttribute(int location);

    void disableAttribute(int location);

    void attributePointer(int location, int components, AttributeType type, boolean normalized,
                          int strideInBytes, int offsetInBytes);

    void integerAttributePointer(int location, int components, AttributeType type,
                                 int strideInBytes, int offsetInBytes);

    void constantAttribute(int location, float x, float y, float z, float w);

    int createTexture();

    void deleteTexture(int texture);

    void activeTexture(int unit);

    void bindTexture(int unit, int texture);

    /** @param rgba at least {@code width * height * 4} bytes; anything past that is left alone */
    void textureImage(int level, int width, int height, byte[] rgba);

    void textureSubImage(int level, int x, int y, int width, int height, byte[] rgba);

    /** the same upload from one 0xAARRGGBB int per pixel, which the platform lays out itself */
    default void textureImageArgb(int level, int width, int height, int[] argb, int count) {
        textureImage(level, width, height, Argb.spread(argb, count));
    }

    default void textureSubImageArgb(int level, int x, int y, int width, int height,
                                     int[] argb, int count) {
        textureSubImage(level, x, y, width, height, Argb.spread(argb, count));
    }

    void generateMipmap();

    void setTextureBaseLevel(int level);

    void setTextureMaxLevel(int level);

    void setTextureMinLod(float lod);

    void setTextureMaxLod(float lod);

    void copyTextureSubImage(int x, int y, int framebufferX, int framebufferY,
                             int width, int height);

    void setTextureMinFilter(TextureFilter filter);

    void setTextureMagFilter(TextureFilter filter);

    void setTextureWrapS(TextureWrap wrap);

    void setTextureWrapT(TextureWrap wrap);

    void drawArrays(Primitive primitive, int first, int count);

    void drawElements(Primitive primitive, int count, IndexType type, int offsetInBytes);

    /** @param instances how many copies to draw, each seeing the divisor-stepped attributes */
    void drawArraysInstanced(Primitive primitive, int first, int count, int instances);

    void drawElementsInstanced(Primitive primitive, int count, IndexType type, int offsetInBytes,
                               int instances);

    /** @param divisor how many instances share one value of the attribute; 0 steps per vertex */
    void attributeDivisor(int location, int divisor);

    int createFramebuffer();

    void bindFramebuffer(int framebuffer);

    void deleteFramebuffer(int framebuffer);

    void framebufferTexture(FramebufferAttachment attachment, int texture, int level);

    void framebufferRenderbuffer(FramebufferAttachment attachment, int renderbuffer);

    FramebufferStatus framebufferStatus();

    int boundFramebuffer();

    int createRenderbuffer();

    void bindRenderbuffer(int renderbuffer);

    void deleteRenderbuffer(int renderbuffer);

    void renderbufferStorage(RenderbufferFormat format, int width, int height);

    void textureStorage(int level, int width, int height);

    byte[] readPixels(int x, int y, int width, int height);

    void finish();

    void flush();

    int width();

    int height();

    GraphicsError takeError();

    String information(GraphicsInfo information);

    int limit(GraphicsLimit limit);
}
