package dev.flagon.platform.browser.graphics;

import dev.flagon.platform.Names;
import dev.flagon.platform.browser.js.JsArrays;
import dev.flagon.platform.graphics.EsContext;

import java.util.HashMap;
import java.util.Map;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Float64Array;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint16Array;
import org.teavm.jso.typedarrays.Uint32Array;
import org.teavm.jso.typedarrays.Uint8Array;

/**
 * {@link EsContext} on the device's webgl2 context, which is gl es 3.0. framebuffer 0 is the
 * device's back buffer and vertex array 0 its default array, so presenting keeps working.
 */
final class WebGLEsContext implements EsContext {

    private static final int FRAMEBUFFER = 0x8D40;
    private static final int READ_FRAMEBUFFER = 0x8CA8;
    private static final int DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int RENDERBUFFER = 0x8D41;
    private static final int SCISSOR_TEST = 0x0C11;
    private static final int RASTERIZER_DISCARD = 0x8C89;
    private static final int COLOR_WRITEMASK = 0x0C23;
    private static final int COLOR_BUFFER_BIT = 0x4000;
    private static final int NEAREST = 0x2600;
    private static final int TIMEOUT_IGNORED = -1;

    private final WebGL2Es gl;
    private final JSObject screen;
    private final JSObject defaultArray;
    private final Names<JSObject> objects = new Names<>();
    private final Names<JSObject> locations = new Names<>();
    private final Map<Integer, Map<String, Integer>> locationNames = new HashMap<>();
    private final String[] extensions;

    private int drawFramebuffer;
    private int readFramebuffer;
    private int renderbuffer;

    WebGLEsContext(JSObject gl, JSObject screen, JSObject defaultArray) {
        this.gl = (WebGL2Es) gl;
        this.screen = screen;
        this.defaultArray = defaultArray;
        stamp(screen, 0);
        stamp(defaultArray, 0);
        JSArray<JSString> supported = this.gl.getSupportedExtensions();
        extensions = new String[supported == null ? 0 : supported.getLength()];
        for (int i = 0; i < extensions.length; i++) {
            extensions[i] = supported.get(i).stringValue();
            // a gl es context has its extensions on from the start; webgl asks first
            this.gl.getExtension(extensions[i]);
        }
    }

    /** blits the back buffer to the canvas around whatever state the program left */
    void present(int width, int height) {
        boolean scissor = gl.isEnabled(SCISSOR_TEST);
        boolean discard = gl.isEnabled(RASTERIZER_DISCARD);
        double[] mask = numbers(gl.getParameter(COLOR_WRITEMASK));
        if (scissor) {
            gl.disable(SCISSOR_TEST);
        }
        if (discard) {
            gl.disable(RASTERIZER_DISCARD);
        }
        gl.colorMask(true, true, true, mask[3] != 0);
        gl.bindFramebuffer(READ_FRAMEBUFFER, screen);
        gl.bindFramebuffer(DRAW_FRAMEBUFFER, null);
        gl.blitFramebuffer(0, 0, width, height, 0, 0, width, height, COLOR_BUFFER_BIT, NEAREST);
        gl.bindFramebuffer(READ_FRAMEBUFFER, framebuffer(readFramebuffer));
        gl.bindFramebuffer(DRAW_FRAMEBUFFER, framebuffer(drawFramebuffer));
        gl.colorMask(mask[0] != 0, mask[1] != 0, mask[2] != 0, mask[3] != 0);
        if (discard) {
            gl.enable(RASTERIZER_DISCARD);
        }
        if (scissor) {
            gl.enable(SCISSOR_TEST);
        }
    }

    /** binds the program's renderbuffer again after the device resized its own */
    void restoreRenderbuffer() {
        gl.bindRenderbuffer(RENDERBUFFER, object(renderbuffer));
    }

    private int add(JSObject object) {
        if (object == null) {
            return 0;
        }
        int name = objects.add(object);
        stamp(object, name);
        return name;
    }

    private JSObject object(int name) {
        return objects.get(name);
    }

    private void remove(int name) {
        objects.remove(name);
    }

    private JSObject framebuffer(int name) {
        return name == 0 ? screen : object(name);
    }

    private JSObject location(int location) {
        return locations.get(location + 1);
    }

    private static double first(JSObject value) {
        double[] read = numbers(value);
        return read.length == 0 ? 0 : read[0];
    }

    private static int[] ints(JSObject value) {
        double[] read = numbers(value);
        int[] values = new int[read.length];
        for (int i = 0; i < read.length; i++) {
            values[i] = (int) (long) read[i];
        }
        return values;
    }

    private static Int8Array bytes(byte[] data) {
        return JsArrays.bytes(data, data.length);
    }

    private static Variable variable(WebGL2Es.ActiveInfo info) {
        return info == null ? null : new Variable(info.getName(), info.getSize(), info.getType());
    }

    private static JSArray<JSString> strings(String[] values) {
        JSArray<JSString> array = new JSArray<>();
        for (String value : values) {
            array.push(JSString.valueOf(value));
        }
        return array;
    }

    @JSBody(params = {"object", "name"}, script = "object.__flagon = name;")
    private static native void stamp(JSObject object, int name);

    @JSBody(params = "value", script = "return typeof value;")
    private static native String typeOf(JSObject value);

    @JSBody(params = "value", script = "return value.__flagon;")
    private static native JSObject nameOf(JSObject value);

    @JSBody(params = "value", script = "return value === undefined;")
    private static native boolean isUndefined(JSObject value);

    @JSBody(params = "value", script = "return value;")
    private static native double asNumber(JSObject value);

    @JSBody(params = "value", script = "return value;")
    private static native boolean asBoolean(JSObject value);

    @JSBody(params = "value", script = "return Float64Array.from(value);")
    private static native Float64Array fromList(JSObject value);

    /** a webgl answer as numbers: a scalar, a boolean, an array, or an object's name */
    private static double[] numbers(JSObject value) {
        // no object bound reads as name 0
        if (value == null || isUndefined(value)) {
            return new double[] {0};
        }
        switch (typeOf(value)) {
            case "number":
                return new double[] {asNumber(value)};
            case "boolean":
                return new double[] {asBoolean(value) ? 1 : 0};
            case "object":
                JSObject name = nameOf(value);
                return isUndefined(name) ? fromList(value).copyToJavaArray()
                        : new double[] {asNumber(name)};
            default:
                return new double[0];
        }
    }

    /** the typed view webgl wants for pixels of a gl type */
    private static ArrayBufferView view(Int8Array bytes, int type) {
        int size = switch (type) {
            case 0x1402, 0x1403, 0x140B, 0x8D61, 0x8363, 0x8033, 0x8034 -> 2;
            case 0x1404, 0x1405, 0x8368, 0x8C3B, 0x8C3E, 0x84FA, 0x1406 -> 4;
            default -> 1;
        };
        Int8Array aligned = bytes;
        if (bytes.getByteOffset() % size != 0) {
            aligned = new Int8Array(bytes.getLength());
            aligned.set(bytes);
        }
        ArrayBuffer buffer = aligned.getBuffer();
        int offset = aligned.getByteOffset();
        int length = aligned.getLength() / size;
        return switch (type) {
            case 0x1400 -> aligned;
            case 0x1402 -> new Int16Array(buffer, offset, length);
            case 0x1403, 0x140B, 0x8D61, 0x8363, 0x8033, 0x8034 ->
                    new Uint16Array(buffer, offset, length);
            case 0x1404 -> new Int32Array(buffer, offset, length);
            case 0x1405, 0x8368, 0x8C3B, 0x8C3E, 0x84FA -> new Uint32Array(buffer, offset, length);
            case 0x1406 -> new Float32Array(buffer, offset, length);
            default -> new Uint8Array(buffer, offset, length);
        };
    }

    @JSBody(params = "values", script = "return Array.from(values);")
    private static native JSArray<JSObject> list(Int32Array values);

    private static JSArray<JSObject> list(int[] values) {
        return list(JsArrays.ints(values, values.length));
    }

    // state

    @Override
    public void enable(int capability) {
        gl.enable(capability);
    }

    @Override
    public void disable(int capability) {
        gl.disable(capability);
    }

    @Override
    public boolean isEnabled(int capability) {
        return gl.isEnabled(capability);
    }

    @Override
    public void activeTexture(int unit) {
        gl.activeTexture(unit);
    }

    @Override
    public void blendColor(float red, float green, float blue, float alpha) {
        gl.blendColor(red, green, blue, alpha);
    }

    @Override
    public void blendEquation(int mode) {
        gl.blendEquation(mode);
    }

    @Override
    public void blendEquationSeparate(int rgb, int alpha) {
        gl.blendEquationSeparate(rgb, alpha);
    }

    @Override
    public void blendFunc(int source, int destination) {
        gl.blendFunc(source, destination);
    }

    @Override
    public void blendFuncSeparate(int sourceRgb, int destinationRgb, int sourceAlpha,
                                  int destinationAlpha) {
        gl.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
    }

    @Override
    public void clear(int mask) {
        gl.clear(mask);
    }

    @Override
    public void clearColor(float red, float green, float blue, float alpha) {
        gl.clearColor(red, green, blue, alpha);
    }

    @Override
    public void clearDepth(float depth) {
        gl.clearDepth(depth);
    }

    @Override
    public void clearStencil(int stencil) {
        gl.clearStencil(stencil);
    }

    @Override
    public void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        gl.colorMask(red, green, blue, alpha);
    }

    @Override
    public void cullFace(int mode) {
        gl.cullFace(mode);
    }

    @Override
    public void frontFace(int mode) {
        gl.frontFace(mode);
    }

    @Override
    public void depthFunc(int function) {
        gl.depthFunc(function);
    }

    @Override
    public void depthMask(boolean flag) {
        gl.depthMask(flag);
    }

    @Override
    public void depthRange(float near, float far) {
        gl.depthRange(near, far);
    }

    @Override
    public void hint(int target, int mode) {
        gl.hint(target, mode);
    }

    @Override
    public void lineWidth(float width) {
        gl.lineWidth(width);
    }

    @Override
    public void pixelStorei(int name, int value) {
        gl.pixelStorei(name, value);
    }

    @Override
    public void polygonOffset(float factor, float units) {
        gl.polygonOffset(factor, units);
    }

    @Override
    public void sampleCoverage(float value, boolean invert) {
        gl.sampleCoverage(value, invert);
    }

    @Override
    public void scissor(int x, int y, int width, int height) {
        gl.scissor(x, y, width, height);
    }

    @Override
    public void viewport(int x, int y, int width, int height) {
        gl.viewport(x, y, width, height);
    }

    @Override
    public void stencilFuncSeparate(int face, int function, int reference, int mask) {
        gl.stencilFuncSeparate(face, function, reference, mask);
    }

    @Override
    public void stencilMaskSeparate(int face, int mask) {
        gl.stencilMaskSeparate(face, mask);
    }

    @Override
    public void stencilOpSeparate(int face, int fail, int depthFail, int pass) {
        gl.stencilOpSeparate(face, fail, depthFail, pass);
    }

    @Override
    public int getError() {
        return gl.getError();
    }

    @Override
    public void finish() {
        gl.finish();
    }

    @Override
    public void flush() {
        gl.flush();
    }

    @Override
    public double[] getParameter(int name) {
        return switch (name) {
            // gl es state webgl leaves out
            case 0x821B -> new double[] {3};
            case 0x821C, 0x8DF9, 0x87FE -> new double[] {0};
            case 0x8DF8, 0x87FF -> new double[0];
            case 0x8DFA -> new double[] {1};
            case 0x821D -> new double[] {extensions.length};
            case 0x86A2 -> new double[] {numbers(gl.getParameter(0x86A3)).length};
            default -> numbers(gl.getParameter(name));
        };
    }

    @Override
    public long getIndexedParameter(int target, int index) {
        return (long) first(gl.getIndexedParameter(target, index));
    }

    @Override
    public String getString(int name) {
        return switch (name) {
            case 0x1F02 -> "OpenGL ES 3.0 " + gl.getParameterString(name);
            case 0x8B8C -> "OpenGL ES GLSL ES 3.00 " + gl.getParameterString(name);
            case 0x1F03 -> String.join(" ", extensions);
            default -> gl.getParameterString(name);
        };
    }

    @Override
    public String[] extensions() {
        return extensions.clone();
    }

    // buffers

    @Override
    public int createBuffer() {
        return add(gl.createBuffer());
    }

    @Override
    public void deleteBuffer(int buffer) {
        gl.deleteBuffer(object(buffer));
        remove(buffer);
    }

    @Override
    public boolean isBuffer(int buffer) {
        return object(buffer) != null && gl.isBuffer(object(buffer));
    }

    @Override
    public void bindBuffer(int target, int buffer) {
        gl.bindBuffer(target, object(buffer));
    }

    @Override
    public void bindBufferBase(int target, int index, int buffer) {
        gl.bindBufferBase(target, index, object(buffer));
    }

    @Override
    public void bindBufferRange(int target, int index, int buffer, long offset, long size) {
        gl.bindBufferRange(target, index, object(buffer), offset, size);
    }

    @Override
    public void bufferData(int target, long size, int usage) {
        gl.bufferData(target, (double) size, usage);
    }

    @Override
    public void bufferData(int target, byte[] data, int usage) {
        gl.bufferData(target, bytes(data), usage);
    }

    @Override
    public void bufferSubData(int target, long offset, byte[] data) {
        gl.bufferSubData(target, offset, bytes(data));
    }

    @Override
    public void getBufferSubData(int target, long offset, byte[] into) {
        Int8Array read = new Int8Array(into.length);
        gl.getBufferSubData(target, offset, read);
        System.arraycopy(JsArrays.bytesOf(read), 0, into, 0, into.length);
    }

    @Override
    public void copyBufferSubData(int readTarget, int writeTarget, long readOffset,
                                  long writeOffset, long size) {
        gl.copyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Override
    public long getBufferParameter(int target, int name) {
        return (long) first(gl.getBufferParameter(target, name));
    }

    // vertex arrays and attributes

    @Override
    public int createVertexArray() {
        return add(gl.createVertexArray());
    }

    @Override
    public void deleteVertexArray(int array) {
        if (array == 0) {
            return;
        }
        boolean bound = numbersOf(0x85B5) == array;
        gl.deleteVertexArray(object(array));
        remove(array);
        if (bound) {
            gl.bindVertexArray(defaultArray);
        }
    }

    @Override
    public boolean isVertexArray(int array) {
        return object(array) != null && gl.isVertexArray(object(array));
    }

    @Override
    public void bindVertexArray(int array) {
        gl.bindVertexArray(array == 0 ? defaultArray : object(array));
    }

    @Override
    public void enableVertexAttribArray(int index) {
        gl.enableVertexAttribArray(index);
    }

    @Override
    public void disableVertexAttribArray(int index) {
        gl.disableVertexAttribArray(index);
    }

    @Override
    public void vertexAttribPointer(int index, int size, int type, boolean normalized,
                                    int stride, long offset) {
        gl.vertexAttribPointer(index, size, type, normalized, stride, offset);
    }

    @Override
    public void vertexAttribIPointer(int index, int size, int type, int stride, long offset) {
        gl.vertexAttribIPointer(index, size, type, stride, offset);
    }

    @Override
    public void vertexAttribDivisor(int index, int divisor) {
        gl.vertexAttribDivisor(index, divisor);
    }

    @Override
    public void vertexAttrib4f(int index, float x, float y, float z, float w) {
        gl.vertexAttrib4f(index, x, y, z, w);
    }

    @Override
    public void vertexAttribI4i(int index, int x, int y, int z, int w) {
        gl.vertexAttribI4i(index, x, y, z, w);
    }

    @Override
    public void vertexAttribI4ui(int index, int x, int y, int z, int w) {
        gl.vertexAttribI4ui(index, x, y, z, w);
    }

    @Override
    public double[] getVertexAttrib(int index, int name) {
        return numbers(gl.getVertexAttrib(index, name));
    }

    @Override
    public long getVertexAttribOffset(int index, int name) {
        return (long) gl.getVertexAttribOffset(index, name);
    }

    // drawing

    @Override
    public void drawArrays(int mode, int first, int count) {
        gl.drawArrays(mode, first, count);
    }

    @Override
    public void drawElements(int mode, int count, int type, long offset) {
        gl.drawElements(mode, count, type, offset);
    }

    @Override
    public void drawArraysInstanced(int mode, int first, int count, int instances) {
        gl.drawArraysInstanced(mode, first, count, instances);
    }

    @Override
    public void drawElementsInstanced(int mode, int count, int type, long offset,
                                      int instances) {
        gl.drawElementsInstanced(mode, count, type, offset, instances);
    }

    @Override
    public void drawRangeElements(int mode, int start, int end, int count, int type,
                                  long offset) {
        gl.drawRangeElements(mode, start, end, count, type, offset);
    }

    // textures

    @Override
    public int createTexture() {
        return add(gl.createTexture());
    }

    @Override
    public void deleteTexture(int texture) {
        gl.deleteTexture(object(texture));
        remove(texture);
    }

    @Override
    public boolean isTexture(int texture) {
        return object(texture) != null && gl.isTexture(object(texture));
    }

    @Override
    public void bindTexture(int target, int texture) {
        gl.bindTexture(target, object(texture));
    }

    @Override
    public void texImage2D(int target, int level, int internalFormat, int width, int height,
                           int border, int format, int type, byte[] data) {
        gl.texImage2D(target, level, internalFormat, width, height, border, format, type,
                data == null ? null : view(bytes(data), type));
    }

    @Override
    public void texImage2D(int target, int level, int internalFormat, int width, int height,
                           int border, int format, int type, long offset) {
        gl.texImage2D(target, level, internalFormat, width, height, border, format, type,
                (double) offset);
    }

    @Override
    public void texSubImage2D(int target, int level, int x, int y, int width, int height,
                              int format, int type, byte[] data) {
        gl.texSubImage2D(target, level, x, y, width, height, format, type,
                view(bytes(data), type));
    }

    @Override
    public void texSubImage2D(int target, int level, int x, int y, int width, int height,
                              int format, int type, long offset) {
        gl.texSubImage2D(target, level, x, y, width, height, format, type, (double) offset);
    }

    @Override
    public void texImage3D(int target, int level, int internalFormat, int width, int height,
                           int depth, int border, int format, int type, byte[] data) {
        gl.texImage3D(target, level, internalFormat, width, height, depth, border, format, type,
                data == null ? null : view(bytes(data), type));
    }

    @Override
    public void texImage3D(int target, int level, int internalFormat, int width, int height,
                           int depth, int border, int format, int type, long offset) {
        gl.texImage3D(target, level, internalFormat, width, height, depth, border, format, type,
                (double) offset);
    }

    @Override
    public void texSubImage3D(int target, int level, int x, int y, int z, int width, int height,
                              int depth, int format, int type, byte[] data) {
        gl.texSubImage3D(target, level, x, y, z, width, height, depth, format, type,
                view(bytes(data), type));
    }

    @Override
    public void texSubImage3D(int target, int level, int x, int y, int z, int width, int height,
                              int depth, int format, int type, long offset) {
        gl.texSubImage3D(target, level, x, y, z, width, height, depth, format, type,
                (double) offset);
    }

    @Override
    public void compressedTexImage2D(int target, int level, int internalFormat, int width,
                                     int height, int border, byte[] data) {
        gl.compressedTexImage2D(target, level, internalFormat, width, height, border,
                bytes(data));
    }

    @Override
    public void compressedTexImage2D(int target, int level, int internalFormat, int width,
                                     int height, int border, int size, long offset) {
        gl.compressedTexImage2D(target, level, internalFormat, width, height, border, size,
                offset);
    }

    @Override
    public void compressedTexSubImage2D(int target, int level, int x, int y, int width,
                                        int height, int format, byte[] data) {
        gl.compressedTexSubImage2D(target, level, x, y, width, height, format, bytes(data));
    }

    @Override
    public void compressedTexSubImage2D(int target, int level, int x, int y, int width,
                                        int height, int format, int size, long offset) {
        gl.compressedTexSubImage2D(target, level, x, y, width, height, format, size, offset);
    }

    @Override
    public void compressedTexImage3D(int target, int level, int internalFormat, int width,
                                     int height, int depth, int border, byte[] data) {
        gl.compressedTexImage3D(target, level, internalFormat, width, height, depth, border,
                bytes(data));
    }

    @Override
    public void compressedTexImage3D(int target, int level, int internalFormat, int width,
                                     int height, int depth, int border, int size, long offset) {
        gl.compressedTexImage3D(target, level, internalFormat, width, height, depth, border,
                size, offset);
    }

    @Override
    public void compressedTexSubImage3D(int target, int level, int x, int y, int z, int width,
                                        int height, int depth, int format, byte[] data) {
        gl.compressedTexSubImage3D(target, level, x, y, z, width, height, depth, format,
                bytes(data));
    }

    @Override
    public void compressedTexSubImage3D(int target, int level, int x, int y, int z, int width,
                                        int height, int depth, int format, int size,
                                        long offset) {
        gl.compressedTexSubImage3D(target, level, x, y, z, width, height, depth, format, size,
                offset);
    }

    @Override
    public void copyTexImage2D(int target, int level, int internalFormat, int x, int y,
                               int width, int height, int border) {
        gl.copyTexImage2D(target, level, internalFormat, x, y, width, height, border);
    }

    @Override
    public void copyTexSubImage2D(int target, int level, int xOffset, int yOffset, int x, int y,
                                  int width, int height) {
        gl.copyTexSubImage2D(target, level, xOffset, yOffset, x, y, width, height);
    }

    @Override
    public void copyTexSubImage3D(int target, int level, int xOffset, int yOffset, int zOffset,
                                  int x, int y, int width, int height) {
        gl.copyTexSubImage3D(target, level, xOffset, yOffset, zOffset, x, y, width, height);
    }

    @Override
    public void texStorage2D(int target, int levels, int internalFormat, int width,
                             int height) {
        gl.texStorage2D(target, levels, internalFormat, width, height);
    }

    @Override
    public void texStorage3D(int target, int levels, int internalFormat, int width, int height,
                             int depth) {
        gl.texStorage3D(target, levels, internalFormat, width, height, depth);
    }

    @Override
    public void generateMipmap(int target) {
        gl.generateMipmap(target);
    }

    @Override
    public void texParameteri(int target, int name, int value) {
        gl.texParameteri(target, name, value);
    }

    @Override
    public void texParameterf(int target, int name, float value) {
        gl.texParameterf(target, name, value);
    }

    @Override
    public double getTexParameter(int target, int name) {
        return first(gl.getTexParameter(target, name));
    }

    // framebuffers and renderbuffers

    @Override
    public int createFramebuffer() {
        return add(gl.createFramebuffer());
    }

    @Override
    public void deleteFramebuffer(int framebuffer) {
        if (framebuffer == 0) {
            return;
        }
        if (drawFramebuffer == framebuffer) {
            drawFramebuffer = 0;
        }
        if (readFramebuffer == framebuffer) {
            readFramebuffer = 0;
        }
        boolean bound = numbersOf(0x8CA6) == framebuffer;
        gl.deleteFramebuffer(object(framebuffer));
        remove(framebuffer);
        if (bound) {
            gl.bindFramebuffer(FRAMEBUFFER, screen);
        }
    }

    private int numbersOf(int name) {
        return (int) first(gl.getParameter(name));
    }

    @Override
    public boolean isFramebuffer(int framebuffer) {
        return object(framebuffer) != null && gl.isFramebuffer(object(framebuffer));
    }

    @Override
    public void bindFramebuffer(int target, int framebuffer) {
        if (target != READ_FRAMEBUFFER) {
            drawFramebuffer = framebuffer;
        }
        if (target != DRAW_FRAMEBUFFER) {
            readFramebuffer = framebuffer;
        }
        gl.bindFramebuffer(target, framebuffer(framebuffer));
    }

    @Override
    public int checkFramebufferStatus(int target) {
        return gl.checkFramebufferStatus(target);
    }

    @Override
    public void framebufferTexture2D(int target, int attachment, int textureTarget,
                                     int texture, int level) {
        gl.framebufferTexture2D(target, attachment, textureTarget, object(texture), level);
    }

    @Override
    public void framebufferTextureLayer(int target, int attachment, int texture, int level,
                                        int layer) {
        gl.framebufferTextureLayer(target, attachment, object(texture), level, layer);
    }

    @Override
    public void framebufferRenderbuffer(int target, int attachment, int renderbufferTarget,
                                        int renderbuffer) {
        gl.framebufferRenderbuffer(target, attachment, renderbufferTarget,
                object(renderbuffer));
    }

    @Override
    public int getFramebufferAttachmentParameter(int target, int attachment, int name) {
        return (int) (long) first(gl.getFramebufferAttachmentParameter(target, attachment,
                name));
    }

    @Override
    public int createRenderbuffer() {
        return add(gl.createRenderbuffer());
    }

    @Override
    public void deleteRenderbuffer(int renderbuffer) {
        if (this.renderbuffer == renderbuffer) {
            this.renderbuffer = 0;
        }
        gl.deleteRenderbuffer(object(renderbuffer));
        remove(renderbuffer);
    }

    @Override
    public boolean isRenderbuffer(int renderbuffer) {
        return object(renderbuffer) != null && gl.isRenderbuffer(object(renderbuffer));
    }

    @Override
    public void bindRenderbuffer(int target, int renderbuffer) {
        this.renderbuffer = renderbuffer;
        gl.bindRenderbuffer(target, object(renderbuffer));
    }

    @Override
    public void renderbufferStorage(int target, int internalFormat, int width, int height) {
        gl.renderbufferStorage(target, internalFormat, width, height);
    }

    @Override
    public void renderbufferStorageMultisample(int target, int samples, int internalFormat,
                                               int width, int height) {
        gl.renderbufferStorageMultisample(target, samples, internalFormat, width, height);
    }

    @Override
    public int getRenderbufferParameter(int target, int name) {
        return (int) first(gl.getRenderbufferParameter(target, name));
    }

    @Override
    public int[] getInternalformatParameter(int target, int internalFormat, int name) {
        return ints(gl.getInternalformatParameter(target, internalFormat, name));
    }

    @Override
    public void blitFramebuffer(int sourceX0, int sourceY0, int sourceX1, int sourceY1,
                                int targetX0, int targetY0, int targetX1, int targetY1,
                                int mask, int filter) {
        gl.blitFramebuffer(sourceX0, sourceY0, sourceX1, sourceY1, targetX0, targetY0, targetX1,
                targetY1, mask, filter);
    }

    @Override
    public void drawBuffers(int[] buffers) {
        gl.drawBuffers(list(buffers));
    }

    @Override
    public void readBuffer(int source) {
        gl.readBuffer(source);
    }

    @Override
    public void invalidateFramebuffer(int target, int[] attachments) {
        gl.invalidateFramebuffer(target, list(attachments));
    }

    @Override
    public void invalidateSubFramebuffer(int target, int[] attachments, int x, int y, int width,
                                         int height) {
        gl.invalidateSubFramebuffer(target, list(attachments), x, y, width, height);
    }

    @Override
    public void clearBufferiv(int buffer, int drawBuffer, int[] values) {
        gl.clearBufferiv(buffer, drawBuffer, JsArrays.ints(values, values.length));
    }

    @Override
    public void clearBufferuiv(int buffer, int drawBuffer, int[] values) {
        gl.clearBufferuiv(buffer, drawBuffer, JsArrays.unsignedInts(values, values.length));
    }

    @Override
    public void clearBufferfv(int buffer, int drawBuffer, float[] values) {
        gl.clearBufferfv(buffer, drawBuffer, JsArrays.floats(values, values.length));
    }

    @Override
    public void clearBufferfi(int buffer, int drawBuffer, float depth, int stencil) {
        gl.clearBufferfi(buffer, drawBuffer, depth, stencil);
    }

    @Override
    public void readPixels(int x, int y, int width, int height, int format, int type,
                           byte[] into) {
        Int8Array read = new Int8Array(into.length);
        gl.readPixels(x, y, width, height, format, type, view(read, type));
        System.arraycopy(JsArrays.bytesOf(read), 0, into, 0, into.length);
    }

    @Override
    public void readPixels(int x, int y, int width, int height, int format, int type,
                           long offset) {
        gl.readPixels(x, y, width, height, format, type, (double) offset);
    }

    // shaders and programs

    @Override
    public int createShader(int type) {
        return add(gl.createShader(type));
    }

    @Override
    public void deleteShader(int shader) {
        gl.deleteShader(object(shader));
        remove(shader);
    }

    @Override
    public boolean isShader(int shader) {
        return object(shader) != null && gl.isShader(object(shader));
    }

    @Override
    public void shaderSource(int shader, String source) {
        gl.shaderSource(object(shader), source);
    }

    @Override
    public void compileShader(int shader) {
        gl.compileShader(object(shader));
    }

    @Override
    public int getShaderParameter(int shader, int name) {
        return switch (name) {
            case 0x8B84 -> lengthWithNul(gl.getShaderInfoLog(object(shader)));
            case 0x8B88 -> lengthWithNul(gl.getShaderSource(object(shader)));
            default -> (int) first(gl.getShaderParameter(object(shader), name));
        };
    }

    private static int lengthWithNul(String text) {
        return text == null || text.isEmpty() ? 0 : text.length() + 1;
    }

    @Override
    public String getShaderInfoLog(int shader) {
        String log = gl.getShaderInfoLog(object(shader));
        return log == null ? "" : log;
    }

    @Override
    public String getShaderSource(int shader) {
        String source = gl.getShaderSource(object(shader));
        return source == null ? "" : source;
    }

    @Override
    public int[] getShaderPrecisionFormat(int shaderType, int precisionType) {
        WebGL2Es.PrecisionFormat format = gl.getShaderPrecisionFormat(shaderType, precisionType);
        return format == null ? new int[3]
                : new int[] {format.getRangeMin(), format.getRangeMax(), format.getPrecision()};
    }

    @Override
    public int createProgram() {
        return add(gl.createProgram());
    }

    @Override
    public void deleteProgram(int program) {
        forgetLocations(program);
        gl.deleteProgram(object(program));
        remove(program);
    }

    @Override
    public boolean isProgram(int program) {
        return object(program) != null && gl.isProgram(object(program));
    }

    @Override
    public void attachShader(int program, int shader) {
        gl.attachShader(object(program), object(shader));
    }

    @Override
    public void detachShader(int program, int shader) {
        gl.detachShader(object(program), object(shader));
    }

    @Override
    public int[] getAttachedShaders(int program) {
        return ints(gl.getAttachedShaders(object(program)));
    }

    @Override
    public void bindAttribLocation(int program, int index, String name) {
        gl.bindAttribLocation(object(program), index, name);
    }

    @Override
    public void linkProgram(int program) {
        forgetLocations(program);
        gl.linkProgram(object(program));
    }

    @Override
    public void validateProgram(int program) {
        gl.validateProgram(object(program));
    }

    @Override
    public void useProgram(int program) {
        gl.useProgram(object(program));
    }

    @Override
    public int getProgramParameter(int program, int name) {
        JSObject held = object(program);
        return switch (name) {
            case 0x8B84 -> lengthWithNul(gl.getProgramInfoLog(held));
            case 0x8B87 -> longestName(held, 0x8B86, false);
            case 0x8B8A -> longestName(held, 0x8B89, true);
            case 0x8C76 -> longestVarying(held);
            case 0x8A35 -> longestBlockName(held);
            case 0x8741, 0x8257 -> 0;
            default -> (int) first(gl.getProgramParameter(held, name));
        };
    }

    private int longestName(JSObject program, int count, boolean attributes) {
        int longest = 0;
        int active = (int) first(gl.getProgramParameter(program, count));
        for (int i = 0; i < active; i++) {
            WebGL2Es.ActiveInfo info = attributes ? gl.getActiveAttrib(program, i)
                    : gl.getActiveUniform(program, i);
            longest = Math.max(longest, info == null ? 0 : info.getName().length() + 1);
        }
        return longest;
    }

    private int longestVarying(JSObject program) {
        int longest = 0;
        int active = (int) first(gl.getProgramParameter(program, 0x8C83));
        for (int i = 0; i < active; i++) {
            WebGL2Es.ActiveInfo info = gl.getTransformFeedbackVarying(program, i);
            longest = Math.max(longest, info == null ? 0 : info.getName().length() + 1);
        }
        return longest;
    }

    private int longestBlockName(JSObject program) {
        int longest = 0;
        int active = (int) first(gl.getProgramParameter(program, 0x8A36));
        for (int i = 0; i < active; i++) {
            longest = Math.max(longest, lengthWithNul(gl.getActiveUniformBlockName(program, i)));
        }
        return longest;
    }

    @Override
    public String getProgramInfoLog(int program) {
        String log = gl.getProgramInfoLog(object(program));
        return log == null ? "" : log;
    }

    @Override
    public int getAttribLocation(int program, String name) {
        return gl.getAttribLocation(object(program), name);
    }

    @Override
    public int getFragDataLocation(int program, String name) {
        return gl.getFragDataLocation(object(program), name);
    }

    @Override
    public Variable getActiveAttrib(int program, int index) {
        return variable(gl.getActiveAttrib(object(program), index));
    }

    @Override
    public Variable getActiveUniform(int program, int index) {
        return variable(gl.getActiveUniform(object(program), index));
    }

    // uniforms

    @Override
    public int getUniformLocation(int program, String name) {
        Map<String, Integer> known = locationNames.computeIfAbsent(program,
                ignored -> new HashMap<>());
        Integer held = known.get(name);
        if (held != null) {
            return held;
        }
        JSObject location = gl.getUniformLocation(object(program), name);
        int made = location == null ? -1 : locations.add(location) - 1;
        known.put(name, made);
        return made;
    }

    /** a relink or delete makes a program's locations stale */
    private void forgetLocations(int program) {
        Map<String, Integer> known = locationNames.remove(program);
        if (known != null) {
            for (int location : known.values()) {
                locations.remove(location + 1);
            }
        }
    }

    @Override
    public void uniform1f(int location, float x) {
        gl.uniform1f(location(location), x);
    }

    @Override
    public void uniform2f(int location, float x, float y) {
        gl.uniform2f(location(location), x, y);
    }

    @Override
    public void uniform3f(int location, float x, float y, float z) {
        gl.uniform3f(location(location), x, y, z);
    }

    @Override
    public void uniform4f(int location, float x, float y, float z, float w) {
        gl.uniform4f(location(location), x, y, z, w);
    }

    @Override
    public void uniform1i(int location, int x) {
        gl.uniform1i(location(location), x);
    }

    @Override
    public void uniform2i(int location, int x, int y) {
        gl.uniform2i(location(location), x, y);
    }

    @Override
    public void uniform3i(int location, int x, int y, int z) {
        gl.uniform3i(location(location), x, y, z);
    }

    @Override
    public void uniform4i(int location, int x, int y, int z, int w) {
        gl.uniform4i(location(location), x, y, z, w);
    }

    @Override
    public void uniform1ui(int location, int x) {
        gl.uniform1ui(location(location), x);
    }

    @Override
    public void uniform2ui(int location, int x, int y) {
        gl.uniform2ui(location(location), x, y);
    }

    @Override
    public void uniform3ui(int location, int x, int y, int z) {
        gl.uniform3ui(location(location), x, y, z);
    }

    @Override
    public void uniform4ui(int location, int x, int y, int z, int w) {
        gl.uniform4ui(location(location), x, y, z, w);
    }

    @Override
    public void uniformfv(int location, int components, float[] values) {
        var data = JsArrays.floats(values, values.length);
        JSObject at = location(location);
        switch (components) {
            case 1 -> gl.uniform1fv(at, data);
            case 2 -> gl.uniform2fv(at, data);
            case 3 -> gl.uniform3fv(at, data);
            default -> gl.uniform4fv(at, data);
        }
    }

    @Override
    public void uniformiv(int location, int components, int[] values) {
        var data = JsArrays.ints(values, values.length);
        JSObject at = location(location);
        switch (components) {
            case 1 -> gl.uniform1iv(at, data);
            case 2 -> gl.uniform2iv(at, data);
            case 3 -> gl.uniform3iv(at, data);
            default -> gl.uniform4iv(at, data);
        }
    }

    @Override
    public void uniformuiv(int location, int components, int[] values) {
        var data = JsArrays.unsignedInts(values, values.length);
        JSObject at = location(location);
        switch (components) {
            case 1 -> gl.uniform1uiv(at, data);
            case 2 -> gl.uniform2uiv(at, data);
            case 3 -> gl.uniform3uiv(at, data);
            default -> gl.uniform4uiv(at, data);
        }
    }

    @Override
    public void uniformMatrixfv(int location, int columns, int rows, boolean transpose,
                                float[] values) {
        var data = JsArrays.floats(values, values.length);
        JSObject at = location(location);
        switch (columns * 10 + rows) {
            case 22 -> gl.uniformMatrix2fv(at, transpose, data);
            case 33 -> gl.uniformMatrix3fv(at, transpose, data);
            case 23 -> gl.uniformMatrix2x3fv(at, transpose, data);
            case 32 -> gl.uniformMatrix3x2fv(at, transpose, data);
            case 24 -> gl.uniformMatrix2x4fv(at, transpose, data);
            case 42 -> gl.uniformMatrix4x2fv(at, transpose, data);
            case 34 -> gl.uniformMatrix3x4fv(at, transpose, data);
            case 43 -> gl.uniformMatrix4x3fv(at, transpose, data);
            default -> gl.uniformMatrix4fv(at, transpose, data);
        }
    }

    @Override
    public double[] getUniform(int program, int location) {
        return numbers(gl.getUniform(object(program), location(location)));
    }

    @Override
    public int getUniformBlockIndex(int program, String name) {
        return gl.getUniformBlockIndex(object(program), name);
    }

    @Override
    public int[] getActiveUniformBlockParameter(int program, int block, int name) {
        return ints(gl.getActiveUniformBlockParameter(object(program), block, name));
    }

    @Override
    public String getActiveUniformBlockName(int program, int block) {
        String name = gl.getActiveUniformBlockName(object(program), block);
        return name == null ? "" : name;
    }

    @Override
    public void uniformBlockBinding(int program, int block, int binding) {
        gl.uniformBlockBinding(object(program), block, binding);
    }

    @Override
    public int[] getUniformIndices(int program, String[] names) {
        int[] indices = ints(gl.getUniformIndices(object(program), strings(names)));
        for (int i = 0; i < indices.length; i++) {
            // webgl answers null where gl answers GL_INVALID_INDEX
            if (indices[i] == 0 && !names[i].isEmpty()
                    && gl.getUniformLocation(object(program), names[i]) == null) {
                indices[i] = -1;
            }
        }
        return indices;
    }

    @Override
    public int[] getActiveUniforms(int program, int[] indices, int name) {
        return ints(gl.getActiveUniforms(object(program), list(indices), name));
    }

    // transform feedback

    @Override
    public int createTransformFeedback() {
        return add(gl.createTransformFeedback());
    }

    @Override
    public void deleteTransformFeedback(int feedback) {
        gl.deleteTransformFeedback(object(feedback));
        remove(feedback);
    }

    @Override
    public boolean isTransformFeedback(int feedback) {
        return object(feedback) != null && gl.isTransformFeedback(object(feedback));
    }

    @Override
    public void bindTransformFeedback(int target, int feedback) {
        gl.bindTransformFeedback(target, object(feedback));
    }

    @Override
    public void beginTransformFeedback(int mode) {
        gl.beginTransformFeedback(mode);
    }

    @Override
    public void endTransformFeedback() {
        gl.endTransformFeedback();
    }

    @Override
    public void pauseTransformFeedback() {
        gl.pauseTransformFeedback();
    }

    @Override
    public void resumeTransformFeedback() {
        gl.resumeTransformFeedback();
    }

    @Override
    public void transformFeedbackVaryings(int program, String[] varyings, int bufferMode) {
        gl.transformFeedbackVaryings(object(program), strings(varyings), bufferMode);
    }

    @Override
    public Variable getTransformFeedbackVarying(int program, int index) {
        return variable(gl.getTransformFeedbackVarying(object(program), index));
    }

    // queries

    @Override
    public int createQuery() {
        return add(gl.createQuery());
    }

    @Override
    public void deleteQuery(int query) {
        gl.deleteQuery(object(query));
        remove(query);
    }

    @Override
    public boolean isQuery(int query) {
        return object(query) != null && gl.isQuery(object(query));
    }

    @Override
    public void beginQuery(int target, int query) {
        gl.beginQuery(target, object(query));
    }

    @Override
    public void endQuery(int target) {
        gl.endQuery(target);
    }

    @Override
    public int getQuery(int target, int name) {
        return (int) first(gl.getQuery(target, name));
    }

    @Override
    public long getQueryParameter(int query, int name) {
        return (long) first(gl.getQueryParameter(object(query), name));
    }

    // samplers

    @Override
    public int createSampler() {
        return add(gl.createSampler());
    }

    @Override
    public void deleteSampler(int sampler) {
        gl.deleteSampler(object(sampler));
        remove(sampler);
    }

    @Override
    public boolean isSampler(int sampler) {
        return object(sampler) != null && gl.isSampler(object(sampler));
    }

    @Override
    public void bindSampler(int unit, int sampler) {
        gl.bindSampler(unit, object(sampler));
    }

    @Override
    public void samplerParameteri(int sampler, int name, int value) {
        gl.samplerParameteri(object(sampler), name, value);
    }

    @Override
    public void samplerParameterf(int sampler, int name, float value) {
        gl.samplerParameterf(object(sampler), name, value);
    }

    @Override
    public double getSamplerParameter(int sampler, int name) {
        return first(gl.getSamplerParameter(object(sampler), name));
    }

    // sync objects

    @Override
    public int fenceSync(int condition, int flags) {
        return add(gl.fenceSync(condition, flags));
    }

    @Override
    public void deleteSync(int sync) {
        gl.deleteSync(object(sync));
        remove(sync);
    }

    @Override
    public boolean isSync(int sync) {
        return object(sync) != null && gl.isSync(object(sync));
    }

    @Override
    public int clientWaitSync(int sync, int flags, long timeout) {
        // webgl allows no wait beyond MAX_CLIENT_WAIT_TIMEOUT_WEBGL, which is 0 in browsers
        return gl.clientWaitSync(object(sync), flags, 0);
    }

    @Override
    public void waitSync(int sync, int flags, long timeout) {
        gl.waitSync(object(sync), flags, TIMEOUT_IGNORED);
    }

    @Override
    public int getSyncParameter(int sync, int name) {
        return (int) first(gl.getSyncParameter(object(sync), name));
    }
}
