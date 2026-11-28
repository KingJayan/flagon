package dev.flagon.platform.browser.graphics;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint32Array;

/** the webgl2 calls {@link WebGLEsContext} makes; objects stay opaque, offsets are numbers */
interface WebGL2Es extends JSObject {

    interface ActiveInfo extends JSObject {

        @JSProperty
        String getName();

        @JSProperty
        int getSize();

        @JSProperty
        int getType();
    }

    interface PrecisionFormat extends JSObject {

        @JSProperty
        int getRangeMin();

        @JSProperty
        int getRangeMax();

        @JSProperty
        int getPrecision();
    }

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

    JSObject getParameter(int name);

    JSObject getIndexedParameter(int target, int index);

    JSArray<JSString> getSupportedExtensions();

    JSObject getExtension(String name);

    JSObject createBuffer();

    void deleteBuffer(JSObject buffer);

    boolean isBuffer(JSObject buffer);

    void bindBuffer(int target, JSObject buffer);

    void bindBufferBase(int target, int index, JSObject buffer);

    void bindBufferRange(int target, int index, JSObject buffer, double offset, double size);

    void bufferData(int target, double size, int usage);

    void bufferData(int target, ArrayBufferView data, int usage);

    void bufferSubData(int target, double offset, ArrayBufferView data);

    void getBufferSubData(int target, double offset, ArrayBufferView into);

    void copyBufferSubData(int readTarget, int writeTarget, double readOffset,
                           double writeOffset, double size);

    JSObject getBufferParameter(int target, int name);

    JSObject createVertexArray();

    void deleteVertexArray(JSObject array);

    boolean isVertexArray(JSObject array);

    void bindVertexArray(JSObject array);

    void enableVertexAttribArray(int index);

    void disableVertexAttribArray(int index);

    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride,
                             double offset);

    void vertexAttribIPointer(int index, int size, int type, int stride, double offset);

    void vertexAttribDivisor(int index, int divisor);

    void vertexAttrib4f(int index, float x, float y, float z, float w);

    void vertexAttribI4i(int index, int x, int y, int z, int w);

    void vertexAttribI4ui(int index, int x, int y, int z, int w);

    JSObject getVertexAttrib(int index, int name);

    double getVertexAttribOffset(int index, int name);

    void drawArrays(int mode, int first, int count);

    void drawElements(int mode, int count, int type, double offset);

    void drawArraysInstanced(int mode, int first, int count, int instances);

    void drawElementsInstanced(int mode, int count, int type, double offset, int instances);

    void drawRangeElements(int mode, int start, int end, int count, int type, double offset);

    JSObject createTexture();

    void deleteTexture(JSObject texture);

    boolean isTexture(JSObject texture);

    void bindTexture(int target, JSObject texture);

    void texImage2D(int target, int level, int internalFormat, int width, int height,
                    int border, int format, int type, ArrayBufferView data);

    void texImage2D(int target, int level, int internalFormat, int width, int height,
                    int border, int format, int type, double offset);

    void texSubImage2D(int target, int level, int x, int y, int width, int height, int format,
                       int type, ArrayBufferView data);

    void texSubImage2D(int target, int level, int x, int y, int width, int height, int format,
                       int type, double offset);

    void texImage3D(int target, int level, int internalFormat, int width, int height, int depth,
                    int border, int format, int type, ArrayBufferView data);

    void texImage3D(int target, int level, int internalFormat, int width, int height, int depth,
                    int border, int format, int type, double offset);

    void texSubImage3D(int target, int level, int x, int y, int z, int width, int height,
                       int depth, int format, int type, ArrayBufferView data);

    void texSubImage3D(int target, int level, int x, int y, int z, int width, int height,
                       int depth, int format, int type, double offset);

    void compressedTexImage2D(int target, int level, int internalFormat, int width, int height,
                              int border, ArrayBufferView data);

    void compressedTexImage2D(int target, int level, int internalFormat, int width, int height,
                              int border, int size, double offset);

    void compressedTexSubImage2D(int target, int level, int x, int y, int width, int height,
                                 int format, ArrayBufferView data);

    void compressedTexSubImage2D(int target, int level, int x, int y, int width, int height,
                                 int format, int size, double offset);

    void compressedTexImage3D(int target, int level, int internalFormat, int width, int height,
                              int depth, int border, ArrayBufferView data);

    void compressedTexImage3D(int target, int level, int internalFormat, int width, int height,
                              int depth, int border, int size, double offset);

    void compressedTexSubImage3D(int target, int level, int x, int y, int z, int width,
                                 int height, int depth, int format, ArrayBufferView data);

    void compressedTexSubImage3D(int target, int level, int x, int y, int z, int width,
                                 int height, int depth, int format, int size, double offset);

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

    JSObject getTexParameter(int target, int name);

    JSObject createFramebuffer();

    void deleteFramebuffer(JSObject framebuffer);

    boolean isFramebuffer(JSObject framebuffer);

    void bindFramebuffer(int target, JSObject framebuffer);

    int checkFramebufferStatus(int target);

    void framebufferTexture2D(int target, int attachment, int textureTarget, JSObject texture,
                              int level);

    void framebufferTextureLayer(int target, int attachment, JSObject texture, int level,
                                 int layer);

    void framebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
                                 JSObject renderbuffer);

    JSObject getFramebufferAttachmentParameter(int target, int attachment, int name);

    JSObject createRenderbuffer();

    void deleteRenderbuffer(JSObject renderbuffer);

    boolean isRenderbuffer(JSObject renderbuffer);

    void bindRenderbuffer(int target, JSObject renderbuffer);

    void renderbufferStorage(int target, int internalFormat, int width, int height);

    void renderbufferStorageMultisample(int target, int samples, int internalFormat, int width,
                                        int height);

    JSObject getRenderbufferParameter(int target, int name);

    JSObject getInternalformatParameter(int target, int internalFormat, int name);

    void blitFramebuffer(int sourceX0, int sourceY0, int sourceX1, int sourceY1, int targetX0,
                         int targetY0, int targetX1, int targetY1, int mask, int filter);

    void drawBuffers(JSArray<JSObject> buffers);

    void readBuffer(int source);

    void invalidateFramebuffer(int target, JSArray<JSObject> attachments);

    void invalidateSubFramebuffer(int target, JSArray<JSObject> attachments, int x, int y,
                                  int width, int height);

    void clearBufferiv(int buffer, int drawBuffer, Int32Array values);

    void clearBufferuiv(int buffer, int drawBuffer, Uint32Array values);

    void clearBufferfv(int buffer, int drawBuffer, Float32Array values);

    void clearBufferfi(int buffer, int drawBuffer, float depth, int stencil);

    void readPixels(int x, int y, int width, int height, int format, int type,
                    ArrayBufferView into);

    void readPixels(int x, int y, int width, int height, int format, int type, double offset);

    JSObject createShader(int type);

    void deleteShader(JSObject shader);

    boolean isShader(JSObject shader);

    void shaderSource(JSObject shader, String source);

    void compileShader(JSObject shader);

    JSObject getShaderParameter(JSObject shader, int name);

    String getShaderInfoLog(JSObject shader);

    String getShaderSource(JSObject shader);

    PrecisionFormat getShaderPrecisionFormat(int shaderType, int precisionType);

    JSObject createProgram();

    void deleteProgram(JSObject program);

    boolean isProgram(JSObject program);

    void attachShader(JSObject program, JSObject shader);

    void detachShader(JSObject program, JSObject shader);

    JSArray<JSObject> getAttachedShaders(JSObject program);

    void bindAttribLocation(JSObject program, int index, String name);

    void linkProgram(JSObject program);

    void validateProgram(JSObject program);

    void useProgram(JSObject program);

    JSObject getProgramParameter(JSObject program, int name);

    String getProgramInfoLog(JSObject program);

    int getAttribLocation(JSObject program, String name);

    int getFragDataLocation(JSObject program, String name);

    ActiveInfo getActiveAttrib(JSObject program, int index);

    ActiveInfo getActiveUniform(JSObject program, int index);

    JSObject getUniformLocation(JSObject program, String name);

    void uniform1f(JSObject location, float x);

    void uniform2f(JSObject location, float x, float y);

    void uniform3f(JSObject location, float x, float y, float z);

    void uniform4f(JSObject location, float x, float y, float z, float w);

    void uniform1i(JSObject location, int x);

    void uniform2i(JSObject location, int x, int y);

    void uniform3i(JSObject location, int x, int y, int z);

    void uniform4i(JSObject location, int x, int y, int z, int w);

    void uniform1ui(JSObject location, int x);

    void uniform2ui(JSObject location, int x, int y);

    void uniform3ui(JSObject location, int x, int y, int z);

    void uniform4ui(JSObject location, int x, int y, int z, int w);

    void uniform1fv(JSObject location, Float32Array values);

    void uniform2fv(JSObject location, Float32Array values);

    void uniform3fv(JSObject location, Float32Array values);

    void uniform4fv(JSObject location, Float32Array values);

    void uniform1iv(JSObject location, Int32Array values);

    void uniform2iv(JSObject location, Int32Array values);

    void uniform3iv(JSObject location, Int32Array values);

    void uniform4iv(JSObject location, Int32Array values);

    void uniform1uiv(JSObject location, Uint32Array values);

    void uniform2uiv(JSObject location, Uint32Array values);

    void uniform3uiv(JSObject location, Uint32Array values);

    void uniform4uiv(JSObject location, Uint32Array values);

    void uniformMatrix2fv(JSObject location, boolean transpose, Float32Array values);

    void uniformMatrix3fv(JSObject location, boolean transpose, Float32Array values);

    void uniformMatrix4fv(JSObject location, boolean transpose, Float32Array values);

    void uniformMatrix2x3fv(JSObject location, boolean transpose, Float32Array values);

    void uniformMatrix3x2fv(JSObject location, boolean transpose, Float32Array values);

    void uniformMatrix2x4fv(JSObject location, boolean transpose, Float32Array values);

    void uniformMatrix4x2fv(JSObject location, boolean transpose, Float32Array values);

    void uniformMatrix3x4fv(JSObject location, boolean transpose, Float32Array values);

    void uniformMatrix4x3fv(JSObject location, boolean transpose, Float32Array values);

    JSObject getUniform(JSObject program, JSObject location);

    int getUniformBlockIndex(JSObject program, String name);

    JSObject getActiveUniformBlockParameter(JSObject program, int block, int name);

    String getActiveUniformBlockName(JSObject program, int block);

    void uniformBlockBinding(JSObject program, int block, int binding);

    JSObject getUniformIndices(JSObject program, JSArray<JSString> names);

    JSObject getActiveUniforms(JSObject program, JSArray<JSObject> indices, int name);

    JSObject createTransformFeedback();

    void deleteTransformFeedback(JSObject feedback);

    boolean isTransformFeedback(JSObject feedback);

    void bindTransformFeedback(int target, JSObject feedback);

    void beginTransformFeedback(int mode);

    void endTransformFeedback();

    void pauseTransformFeedback();

    void resumeTransformFeedback();

    void transformFeedbackVaryings(JSObject program, JSArray<JSString> varyings,
                                   int bufferMode);

    ActiveInfo getTransformFeedbackVarying(JSObject program, int index);

    JSObject createQuery();

    void deleteQuery(JSObject query);

    boolean isQuery(JSObject query);

    void beginQuery(int target, JSObject query);

    void endQuery(int target);

    JSObject getQuery(int target, int name);

    JSObject getQueryParameter(JSObject query, int name);

    JSObject createSampler();

    void deleteSampler(JSObject sampler);

    boolean isSampler(JSObject sampler);

    void bindSampler(int unit, JSObject sampler);

    void samplerParameteri(JSObject sampler, int name, int value);

    void samplerParameterf(JSObject sampler, int name, float value);

    JSObject getSamplerParameter(JSObject sampler, int name);

    JSObject fenceSync(int condition, int flags);

    void deleteSync(JSObject sync);

    boolean isSync(JSObject sync);

    int clientWaitSync(JSObject sync, int flags, double timeout);

    void waitSync(JSObject sync, int flags, double timeout);

    JSObject getSyncParameter(JSObject sync, int name);

    @JSMethod("getParameter")
    String getParameterString(int name);
}
