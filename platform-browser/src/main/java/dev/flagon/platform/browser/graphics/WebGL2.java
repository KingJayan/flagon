package dev.flagon.platform.browser.graphics;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.typedarrays.Uint32Array;

public interface WebGL2 extends JSObject {

    int FRAMEBUFFER = 0x8D40;
    int DRAW_FRAMEBUFFER = 0x8CA9;
    int RENDERBUFFER = 0x8D41;
    int COLOR_ATTACHMENT0 = 0x8CE0;
    int DEPTH_ATTACHMENT = 0x8D00;
    int STENCIL_ATTACHMENT = 0x8D20;
    int DEPTH_STENCIL_ATTACHMENT = 0x821A;
    int FRAMEBUFFER_COMPLETE = 0x8CD5;
    int FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6;
    int FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
    int FRAMEBUFFER_INCOMPLETE_DIMENSIONS = 0x8CD9;
    int FRAMEBUFFER_UNSUPPORTED = 0x8CDD;
    int RGBA8 = 0x8058;
    int DEPTH_COMPONENT16 = 0x81A5;
    int DEPTH_COMPONENT24 = 0x81A6;
    int DEPTH24_STENCIL8 = 0x88F0;
    int STENCIL_INDEX8 = 0x8D48;

    int DEPTH_BUFFER_BIT = 0x0100;
    int STENCIL_BUFFER_BIT = 0x0400;
    int STENCIL_TEST = 0x0B90;
    int POLYGON_OFFSET_FILL = 0x8037;
    int KEEP = 0x1E00;
    int REPLACE = 0x1E01;
    int INCR = 0x1E02;
    int DECR = 0x1E03;
    int INVERT = 0x150A;
    int INCR_WRAP = 0x8507;
    int DECR_WRAP = 0x8508;
    int COLOR_BUFFER_BIT = 0x4000;

    int POINTS = 0x0000;
    int LINES = 0x0001;
    int LINE_LOOP = 0x0002;
    int LINE_STRIP = 0x0003;
    int TRIANGLES = 0x0004;
    int TRIANGLE_STRIP = 0x0005;
    int TRIANGLE_FAN = 0x0006;

    int ZERO = 0;
    int ONE = 1;
    int SRC_COLOR = 0x0300;
    int ONE_MINUS_SRC_COLOR = 0x0301;
    int SRC_ALPHA = 0x0302;
    int ONE_MINUS_SRC_ALPHA = 0x0303;
    int DST_ALPHA = 0x0304;
    int ONE_MINUS_DST_ALPHA = 0x0305;
    int DST_COLOR = 0x0306;
    int ONE_MINUS_DST_COLOR = 0x0307;
    int SRC_ALPHA_SATURATE = 0x0308;
    int CONSTANT_COLOR = 0x8001;
    int ONE_MINUS_CONSTANT_COLOR = 0x8002;
    int CONSTANT_ALPHA = 0x8003;
    int ONE_MINUS_CONSTANT_ALPHA = 0x8004;

    int FUNC_ADD = 0x8006;
    int FUNC_SUBTRACT = 0x800A;
    int FUNC_REVERSE_SUBTRACT = 0x800B;
    int MIN = 0x8007;
    int MAX = 0x8008;

    int NEVER = 0x0200;
    int LESS = 0x0201;
    int EQUAL = 0x0202;
    int LEQUAL = 0x0203;
    int GREATER = 0x0204;
    int NOTEQUAL = 0x0205;
    int GEQUAL = 0x0206;
    int ALWAYS = 0x0207;

    int CULL_FACE = 0x0B44;
    int DEPTH_TEST = 0x0B71;
    int BLEND = 0x0BE2;

    int TEXTURE_2D = 0x0DE1;
    int BYTE = 0x1400;
    int UNSIGNED_BYTE = 0x1401;
    int SHORT = 0x1402;
    int UNSIGNED_SHORT = 0x1403;
    int INT = 0x1404;
    int UNSIGNED_INT = 0x1405;
    int FLOAT = 0x1406;
    int RGBA = 0x1908;

    int NEAREST = 0x2600;
    int LINEAR = 0x2601;
    int TEXTURE_MAG_FILTER = 0x2800;
    int TEXTURE_MIN_FILTER = 0x2801;
    int TEXTURE_WRAP_S = 0x2802;
    int TEXTURE_WRAP_T = 0x2803;
    int REPEAT = 0x2901;
    int CLAMP_TO_EDGE = 0x812F;
    int MIRRORED_REPEAT = 0x8370;
    int NEAREST_MIPMAP_NEAREST = 0x2700;
    int LINEAR_MIPMAP_NEAREST = 0x2701;
    int NEAREST_MIPMAP_LINEAR = 0x2702;
    int LINEAR_MIPMAP_LINEAR = 0x2703;
    int TEXTURE_BASE_LEVEL = 0x813C;
    int TEXTURE_MAX_LEVEL = 0x813D;
    int TEXTURE_MIN_LOD = 0x813A;
    int TEXTURE_MAX_LOD = 0x813B;
    int TEXTURE0 = 0x84C0;

    int ARRAY_BUFFER = 0x8892;
    int ELEMENT_ARRAY_BUFFER = 0x8893;
    int STREAM_DRAW = 0x88E0;
    int STATIC_DRAW = 0x88E4;
    int DYNAMIC_DRAW = 0x88E8;

    int FRAGMENT_SHADER = 0x8B30;
    int VERTEX_SHADER = 0x8B31;
    int COMPILE_STATUS = 0x8B81;
    int LINK_STATUS = 0x8B82;
    int VALIDATE_STATUS = 0x8B83;
    int MAX_VERTEX_ATTRIBS = 0x8869;
    int MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    int MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49;
    int MAX_VARYING_COMPONENTS = 0x8B4B;
    int MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C;
    int MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
    int MAX_ARRAY_TEXTURE_LAYERS = 0x88FF;
    int INVALID_ENUM = 0x0500;
    int INVALID_VALUE = 0x0501;
    int INVALID_OPERATION = 0x0502;
    int OUT_OF_MEMORY = 0x0505;
    int INVALID_FRAMEBUFFER_OPERATION = 0x0506;
    int MAX_TEXTURE_SIZE = 0x0D33;
    int MAX_TEXTURE_IMAGE_UNITS = 0x8872;
    int VENDOR = 0x1F00;
    int RENDERER = 0x1F01;
    int VERSION = 0x1F02;
    int SHADING_LANGUAGE_VERSION = 0x8B8C;
    int FRONT = 0x0404;
    int BACK = 0x0405;
    int FRONT_AND_BACK = 0x0408;
    int CW = 0x0900;
    int CCW = 0x0901;
    int SCISSOR_TEST = 0x0C11;

    int UNPACK_PREMULTIPLY_ALPHA_WEBGL = 0x9241;

    void viewport(int x, int y, int width, int height);

    void clearColor(float red, float green, float blue, float alpha);

    void clearDepth(float depth);

    void clear(int mask);

    void enable(int capability);

    void disable(int capability);

    void depthFunc(int func);

    void depthMask(boolean enabled);

    void blendFunc(int source, int destination);

    void blendFuncSeparate(int sourceColor, int destinationColor,
                           int sourceAlpha, int destinationAlpha);

    void blendEquation(int equation);

    void blendEquationSeparate(int color, int alpha);

    void blendColor(float red, float green, float blue, float alpha);

    void colorMask(boolean red, boolean green, boolean blue, boolean alpha);

    void pixelStorei(int name, int value);

    JSObject createBuffer();

    void deleteBuffer(JSObject buffer);

    void bindBuffer(int target, JSObject buffer);

    void bufferData(int target, int sizeInBytes, int usage);

    void bufferData(int target, Int8Array data, int usage, int sourceOffset, int length);

    void bufferData(int target, Float32Array data, int usage, int sourceOffset, int length);

    void bufferData(int target, Uint32Array data, int usage, int sourceOffset, int length);

    void bufferSubData(int target, int offsetInBytes, Int8Array data, int sourceOffset, int length);

    void bufferSubData(int target, int offsetInBytes, Float32Array data, int sourceOffset,
            int length);

    JSObject createShader(int type);

    void shaderSource(JSObject shader, String source);

    void compileShader(JSObject shader);

    boolean getShaderParameter(JSObject shader, int name);

    String getShaderInfoLog(JSObject shader);

    void deleteShader(JSObject shader);

    JSObject createProgram();

    void attachShader(JSObject program, JSObject shader);

    void detachShader(JSObject program, JSObject shader);

    boolean isShader(JSObject shader);

    boolean isProgram(JSObject program);

    void validateProgram(JSObject program);

    void bindAttribLocation(JSObject program, int index, String name);

    void linkProgram(JSObject program);

    boolean getProgramParameter(JSObject program, int name);

    String getProgramInfoLog(JSObject program);

    void deleteProgram(JSObject program);

    void useProgram(JSObject program);

    JSObject getUniformLocation(JSObject program, String name);

    void uniformMatrix4fv(JSObject location, boolean transpose, Float32Array value);

    void uniformMatrix2fv(JSObject location, boolean transpose, Float32Array value);

    void uniformMatrix3fv(JSObject location, boolean transpose, Float32Array value);

    void uniform1i(JSObject location, int value);

    void uniform1f(JSObject location, float value);

    void uniform2f(JSObject location, float x, float y);

    void uniform3f(JSObject location, float x, float y, float z);

    void uniform4f(JSObject location, float x, float y, float z, float w);

    void uniform2i(JSObject location, int x, int y);

    void uniform3i(JSObject location, int x, int y, int z);

    void uniform4i(JSObject location, int x, int y, int z, int w);

    void uniform1fv(JSObject location, Float32Array value);

    void uniform2fv(JSObject location, Float32Array value);

    void uniform3fv(JSObject location, Float32Array value);

    void uniform4fv(JSObject location, Float32Array value);

    void uniform1iv(JSObject location, Int32Array value);

    void uniform2iv(JSObject location, Int32Array value);

    void uniform3iv(JSObject location, Int32Array value);

    void uniform4iv(JSObject location, Int32Array value);

    int getAttribLocation(JSObject program, String name);

    void enableVertexAttribArray(int index);

    void disableVertexAttribArray(int index);

    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride,
            int offset);

    void vertexAttribIPointer(int index, int size, int type, int stride, int offset);

    void vertexAttrib4f(int index, float x, float y, float z, float w);

    JSObject createVertexArray();

    void bindVertexArray(JSObject array);

    void deleteVertexArray(JSObject array);

    JSObject createTexture();

    void deleteTexture(JSObject texture);

    void activeTexture(int unit);

    void bindTexture(int target, JSObject texture);

    void texImage2D(int target, int level, int internalFormat, int width, int height,
                    int border, int format, int type, Uint8Array pixels);

    void texSubImage2D(int target, int level, int x, int y, int width, int height,
                       int format, int type, Uint8Array pixels);

    void texParameteri(int target, int name, int value);

    void texParameterf(int target, int name, float value);

    void stencilFunc(int func, int reference, int mask);

    void stencilOp(int stencilFail, int depthFail, int pass);

    void stencilMask(int mask);

    void clearStencil(int value);

    void polygonOffset(float factor, float units);

    void lineWidth(float width);

    void copyTexSubImage2D(int target, int level, int x, int y,
                           int framebufferX, int framebufferY, int width, int height);

    void readPixels(int x, int y, int width, int height, int format, int type, Uint8Array pixels);

    void blitFramebuffer(int sourceX0, int sourceY0, int sourceX1, int sourceY1,
                         int targetX0, int targetY0, int targetX1, int targetY1,
                         int mask, int filter);

    JSObject createFramebuffer();

    void bindFramebuffer(int target, JSObject framebuffer);

    void deleteFramebuffer(JSObject framebuffer);

    void framebufferTexture2D(int target, int attachment, int textureTarget, JSObject texture,
                              int level);

    void framebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
                                 JSObject renderbuffer);

    int checkFramebufferStatus(int target);

    JSObject createRenderbuffer();

    void bindRenderbuffer(int target, JSObject renderbuffer);

    void deleteRenderbuffer(JSObject renderbuffer);

    void renderbufferStorage(int target, int format, int width, int height);

    void finish();

    void flush();

    void generateMipmap(int target);

    void cullFace(int face);

    void frontFace(int winding);

    void scissor(int x, int y, int width, int height);

    void drawArrays(int mode, int first, int count);

    void drawElements(int mode, int count, int type, int offset);

    void drawArraysInstanced(int mode, int first, int count, int instances);

    void drawElementsInstanced(int mode, int count, int type, int offset, int instances);

    void vertexAttribDivisor(int location, int divisor);

    int getParameter(int name);

    @JSMethod("getParameter")
    String getParameterString(int name);

    JSArray<JSString> getSupportedExtensions();

    int getError();

    @JSProperty
    int getDrawingBufferWidth();

    @JSProperty
    int getDrawingBufferHeight();
}
