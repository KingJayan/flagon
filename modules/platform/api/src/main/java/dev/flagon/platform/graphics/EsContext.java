package dev.flagon.platform.graphics;

/**
 * a gl es 3.0 context, call for call. values are gl es enums, objects are int names with 0 for
 * none, and pixel and buffer data are native-order bytes. framebuffer 0 and vertex array 0 are
 * the defaults the device draws to the screen with.
 */
public interface EsContext {

    /** a program's attribute, uniform or transform feedback varying */
    record Variable(String name, int size, int type) {
    }

    // state

    void enable(int capability);

    void disable(int capability);

    boolean isEnabled(int capability);

    void activeTexture(int unit);

    void blendColor(float red, float green, float blue, float alpha);

    void blendEquation(int mode);

    void blendEquationSeparate(int rgb, int alpha);

    void blendFunc(int source, int destination);

    void blendFuncSeparate(int sourceRgb, int destinationRgb, int sourceAlpha,
                           int destinationAlpha);

    void clear(int mask);

    void clearColor(float red, float green, float blue, float alpha);

    void clearDepth(float depth);

    void clearStencil(int stencil);

    void colorMask(boolean red, boolean green, boolean blue, boolean alpha);

    void cullFace(int mode);

    void frontFace(int mode);

    void depthFunc(int function);

    void depthMask(boolean flag);

    void depthRange(float near, float far);

    void hint(int target, int mode);

    void lineWidth(float width);

    void pixelStorei(int name, int value);

    void polygonOffset(float factor, float units);

    void sampleCoverage(float value, boolean invert);

    void scissor(int x, int y, int width, int height);

    void viewport(int x, int y, int width, int height);

    void stencilFuncSeparate(int face, int function, int reference, int mask);

    void stencilMaskSeparate(int face, int mask);

    void stencilOpSeparate(int face, int fail, int depthFail, int pass);

    int getError();

    void finish();

    void flush();

    /** a state value as numbers: one for a scalar, several for a vector, a name for an object */
    double[] getParameter(int name);

    long getIndexedParameter(int target, int index);

    String getString(int name);

    String[] extensions();

    // buffers

    int createBuffer();

    void deleteBuffer(int buffer);

    boolean isBuffer(int buffer);

    void bindBuffer(int target, int buffer);

    void bindBufferBase(int target, int index, int buffer);

    void bindBufferRange(int target, int index, int buffer, long offset, long size);

    void bufferData(int target, long size, int usage);

    void bufferData(int target, byte[] data, int usage);

    void bufferSubData(int target, long offset, byte[] data);

    void getBufferSubData(int target, long offset, byte[] into);

    void copyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset,
                           long size);

    long getBufferParameter(int target, int name);

    // vertex arrays and attributes

    int createVertexArray();

    void deleteVertexArray(int array);

    boolean isVertexArray(int array);

    void bindVertexArray(int array);

    void enableVertexAttribArray(int index);

    void disableVertexAttribArray(int index);

    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride,
                             long offset);

    void vertexAttribIPointer(int index, int size, int type, int stride, long offset);

    void vertexAttribDivisor(int index, int divisor);

    void vertexAttrib4f(int index, float x, float y, float z, float w);

    void vertexAttribI4i(int index, int x, int y, int z, int w);

    void vertexAttribI4ui(int index, int x, int y, int z, int w);

    double[] getVertexAttrib(int index, int name);

    long getVertexAttribOffset(int index, int name);

    // drawing

    void drawArrays(int mode, int first, int count);

    void drawElements(int mode, int count, int type, long offset);

    void drawArraysInstanced(int mode, int first, int count, int instances);

    void drawElementsInstanced(int mode, int count, int type, long offset, int instances);

    void drawRangeElements(int mode, int start, int end, int count, int type, long offset);

    // textures

    int createTexture();

    void deleteTexture(int texture);

    boolean isTexture(int texture);

    void bindTexture(int target, int texture);

    /** data is null to reserve storage */
    void texImage2D(int target, int level, int internalFormat, int width, int height,
                    int border, int format, int type, byte[] data);

    /** reads the pixels from the bound pixel unpack buffer */
    void texImage2D(int target, int level, int internalFormat, int width, int height,
                    int border, int format, int type, long offset);

    void texSubImage2D(int target, int level, int x, int y, int width, int height, int format,
                       int type, byte[] data);

    void texSubImage2D(int target, int level, int x, int y, int width, int height, int format,
                       int type, long offset);

    void texImage3D(int target, int level, int internalFormat, int width, int height, int depth,
                    int border, int format, int type, byte[] data);

    void texImage3D(int target, int level, int internalFormat, int width, int height, int depth,
                    int border, int format, int type, long offset);

    void texSubImage3D(int target, int level, int x, int y, int z, int width, int height,
                       int depth, int format, int type, byte[] data);

    void texSubImage3D(int target, int level, int x, int y, int z, int width, int height,
                       int depth, int format, int type, long offset);

    void compressedTexImage2D(int target, int level, int internalFormat, int width, int height,
                              int border, byte[] data);

    void compressedTexImage2D(int target, int level, int internalFormat, int width, int height,
                              int border, int size, long offset);

    void compressedTexSubImage2D(int target, int level, int x, int y, int width, int height,
                                 int format, byte[] data);

    void compressedTexSubImage2D(int target, int level, int x, int y, int width, int height,
                                 int format, int size, long offset);

    void compressedTexImage3D(int target, int level, int internalFormat, int width, int height,
                              int depth, int border, byte[] data);

    void compressedTexImage3D(int target, int level, int internalFormat, int width, int height,
                              int depth, int border, int size, long offset);

    void compressedTexSubImage3D(int target, int level, int x, int y, int z, int width,
                                 int height, int depth, int format, byte[] data);

    void compressedTexSubImage3D(int target, int level, int x, int y, int z, int width,
                                 int height, int depth, int format, int size, long offset);

    void copyTexImage2D(int target, int level, int internalFormat, int x, int y, int width,
                        int height, int border);

    void copyTexSubImage2D(int target, int level, int xOffset, int yOffset, int x, int y,
                           int width, int height);

    void copyTexSubImage3D(int target, int level, int xOffset, int yOffset, int zOffset, int x,
                           int y, int width, int height);

    void texStorage2D(int target, int levels, int internalFormat, int width, int height);

    void texStorage3D(int target, int levels, int internalFormat, int width, int height,
                      int depth);

    void generateMipmap(int target);

    void texParameteri(int target, int name, int value);

    void texParameterf(int target, int name, float value);

    double getTexParameter(int target, int name);

    // framebuffers and renderbuffers

    int createFramebuffer();

    void deleteFramebuffer(int framebuffer);

    boolean isFramebuffer(int framebuffer);

    void bindFramebuffer(int target, int framebuffer);

    int checkFramebufferStatus(int target);

    void framebufferTexture2D(int target, int attachment, int textureTarget, int texture,
                              int level);

    void framebufferTextureLayer(int target, int attachment, int texture, int level, int layer);

    void framebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
                                 int renderbuffer);

    int getFramebufferAttachmentParameter(int target, int attachment, int name);

    int createRenderbuffer();

    void deleteRenderbuffer(int renderbuffer);

    boolean isRenderbuffer(int renderbuffer);

    void bindRenderbuffer(int target, int renderbuffer);

    void renderbufferStorage(int target, int internalFormat, int width, int height);

    void renderbufferStorageMultisample(int target, int samples, int internalFormat, int width,
                                        int height);

    int getRenderbufferParameter(int target, int name);

    int[] getInternalformatParameter(int target, int internalFormat, int name);

    void blitFramebuffer(int sourceX0, int sourceY0, int sourceX1, int sourceY1, int targetX0,
                         int targetY0, int targetX1, int targetY1, int mask, int filter);

    void drawBuffers(int[] buffers);

    void readBuffer(int source);

    void invalidateFramebuffer(int target, int[] attachments);

    void invalidateSubFramebuffer(int target, int[] attachments, int x, int y, int width,
                                  int height);

    void clearBufferiv(int buffer, int drawBuffer, int[] values);

    void clearBufferuiv(int buffer, int drawBuffer, int[] values);

    void clearBufferfv(int buffer, int drawBuffer, float[] values);

    void clearBufferfi(int buffer, int drawBuffer, float depth, int stencil);

    void readPixels(int x, int y, int width, int height, int format, int type, byte[] into);

    /** writes the pixels to the bound pixel pack buffer */
    void readPixels(int x, int y, int width, int height, int format, int type, long offset);

    // shaders and programs

    int createShader(int type);

    void deleteShader(int shader);

    boolean isShader(int shader);

    void shaderSource(int shader, String source);

    void compileShader(int shader);

    int getShaderParameter(int shader, int name);

    String getShaderInfoLog(int shader);

    String getShaderSource(int shader);

    /** range min, range max and precision */
    int[] getShaderPrecisionFormat(int shaderType, int precisionType);

    int createProgram();

    void deleteProgram(int program);

    boolean isProgram(int program);

    void attachShader(int program, int shader);

    void detachShader(int program, int shader);

    int[] getAttachedShaders(int program);

    void bindAttribLocation(int program, int index, String name);

    void linkProgram(int program);

    void validateProgram(int program);

    void useProgram(int program);

    int getProgramParameter(int program, int name);

    String getProgramInfoLog(int program);

    int getAttribLocation(int program, String name);

    int getFragDataLocation(int program, String name);

    Variable getActiveAttrib(int program, int index);

    Variable getActiveUniform(int program, int index);

    // uniforms

    /** -1 when the program has no active uniform of that name */
    int getUniformLocation(int program, String name);

    void uniform1f(int location, float x);

    void uniform2f(int location, float x, float y);

    void uniform3f(int location, float x, float y, float z);

    void uniform4f(int location, float x, float y, float z, float w);

    void uniform1i(int location, int x);

    void uniform2i(int location, int x, int y);

    void uniform3i(int location, int x, int y, int z);

    void uniform4i(int location, int x, int y, int z, int w);

    void uniform1ui(int location, int x);

    void uniform2ui(int location, int x, int y);

    void uniform3ui(int location, int x, int y, int z);

    void uniform4ui(int location, int x, int y, int z, int w);

    /** components is 1 to 4 */
    void uniformfv(int location, int components, float[] values);

    void uniformiv(int location, int components, int[] values);

    void uniformuiv(int location, int components, int[] values);

    void uniformMatrixfv(int location, int columns, int rows, boolean transpose,
                         float[] values);

    double[] getUniform(int program, int location);

    int getUniformBlockIndex(int program, String name);

    /** one value, or the uniform indices for GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES */
    int[] getActiveUniformBlockParameter(int program, int block, int name);

    String getActiveUniformBlockName(int program, int block);

    void uniformBlockBinding(int program, int block, int binding);

    int[] getUniformIndices(int program, String[] names);

    int[] getActiveUniforms(int program, int[] indices, int name);

    // transform feedback

    int createTransformFeedback();

    void deleteTransformFeedback(int feedback);

    boolean isTransformFeedback(int feedback);

    void bindTransformFeedback(int target, int feedback);

    void beginTransformFeedback(int mode);

    void endTransformFeedback();

    void pauseTransformFeedback();

    void resumeTransformFeedback();

    void transformFeedbackVaryings(int program, String[] varyings, int bufferMode);

    Variable getTransformFeedbackVarying(int program, int index);

    // queries

    int createQuery();

    void deleteQuery(int query);

    boolean isQuery(int query);

    void beginQuery(int target, int query);

    void endQuery(int target);

    int getQuery(int target, int name);

    /** GL_QUERY_RESULT reads 0 until GL_QUERY_RESULT_AVAILABLE is true */
    long getQueryParameter(int query, int name);

    // samplers

    int createSampler();

    void deleteSampler(int sampler);

    boolean isSampler(int sampler);

    void bindSampler(int unit, int sampler);

    void samplerParameteri(int sampler, int name, int value);

    void samplerParameterf(int sampler, int name, float value);

    double getSamplerParameter(int sampler, int name);

    // sync objects

    int fenceSync(int condition, int flags);

    void deleteSync(int sync);

    boolean isSync(int sync);

    /** waits no longer than the host allows; the caller polls for the rest */
    int clientWaitSync(int sync, int flags, long timeout);

    void waitSync(int sync, int flags, long timeout);

    int getSyncParameter(int sync, int name);
}
