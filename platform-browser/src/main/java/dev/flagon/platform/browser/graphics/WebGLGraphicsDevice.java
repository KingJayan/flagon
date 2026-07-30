package dev.flagon.platform.browser.graphics;

import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.Primitive;
import dev.flagon.platform.graphics.ShaderException;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint32Array;
import org.teavm.jso.typedarrays.Uint8Array;

import java.util.ArrayList;
import java.util.List;

/** {@link GraphicsDevice} on a WebGL2 context. */
public final class WebGLGraphicsDevice implements GraphicsDevice {

    private final WebGL2 gl;

    // gl names are ints; webgl hands back objects. index 0 stays empty so 0 means "none",
    // matching gl's convention.
    private final List<JSObject> handles = new ArrayList<>();
    private final List<JSObject> uniforms = new ArrayList<>();

    private Float32Array scratch = Float32Array.create(0);
    private String pendingError;

    public WebGLGraphicsDevice(WebGL2 gl) {
        this.gl = gl;
        handles.add(null);
        uniforms.add(null);
        // webgl2 core profile requires a bound vertex array object before any attribute is set
        gl.bindVertexArray(gl.createVertexArray());
    }

    private int store(JSObject object) {
        handles.add(object);
        return handles.size() - 1;
    }

    private JSObject lookup(int name, String kind) {
        if (name <= 0 || name >= handles.size() || handles.get(name) == null) {
            throw new IllegalArgumentException("not a live " + kind + ": " + name);
        }
        return handles.get(name);
    }

    @Override
    public void viewport(int x, int y, int width, int height) {
        gl.viewport(x, y, width, height);
    }

    @Override
    public void clearColor(float red, float green, float blue, float alpha) {
        gl.clearColor(red, green, blue, alpha);
    }

    @Override
    public void clearDepth(double depth) {
        gl.clearDepth((float) depth);
    }

    @Override
    public void clear(boolean color, boolean depth, boolean stencil) {
        int mask = 0;
        if (color) {
            mask |= WebGL2.COLOR_BUFFER_BIT;
        }
        if (depth) {
            mask |= WebGL2.DEPTH_BUFFER_BIT;
        }
        if (stencil) {
            mask |= WebGL2.STENCIL_BUFFER_BIT;
        }
        if (mask != 0) {
            gl.clear(mask);
        }
    }

    @Override
    public void setDepthTest(boolean enabled) {
        capability(WebGL2.DEPTH_TEST, enabled);
    }

    @Override
    public void setDepthWrite(boolean enabled) {
        gl.depthMask(enabled);
    }

    @Override
    public void setDepthFunc(CompareFunc func) {
        gl.depthFunc(switch (func) {
            case NEVER -> WebGL2.NEVER;
            case LESS -> WebGL2.LESS;
            case EQUAL -> WebGL2.EQUAL;
            case LESS_OR_EQUAL -> WebGL2.LEQUAL;
            case GREATER -> WebGL2.GREATER;
            case NOT_EQUAL -> WebGL2.NOTEQUAL;
            case GREATER_OR_EQUAL -> WebGL2.GEQUAL;
            case ALWAYS -> WebGL2.ALWAYS;
        });
    }

    @Override
    public void setBlend(boolean enabled) {
        capability(WebGL2.BLEND, enabled);
    }

    @Override
    public void setBlendFunc(BlendFactor source, BlendFactor destination) {
        gl.blendFunc(blendFactor(source), blendFactor(destination));
    }

    @Override
    public void setCullFace(boolean enabled) {
        capability(WebGL2.CULL_FACE, enabled);
    }

    @Override
    public void setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        gl.colorMask(red, green, blue, alpha);
    }

    private void capability(int capability, boolean enabled) {
        if (enabled) {
            gl.enable(capability);
        } else {
            gl.disable(capability);
        }
    }

    private static int blendFactor(BlendFactor factor) {
        return switch (factor) {
            case ZERO -> WebGL2.ZERO;
            case ONE -> WebGL2.ONE;
            case SRC_COLOR -> WebGL2.SRC_COLOR;
            case ONE_MINUS_SRC_COLOR -> WebGL2.ONE_MINUS_SRC_COLOR;
            case DST_COLOR -> WebGL2.DST_COLOR;
            case ONE_MINUS_DST_COLOR -> WebGL2.ONE_MINUS_DST_COLOR;
            case SRC_ALPHA -> WebGL2.SRC_ALPHA;
            case ONE_MINUS_SRC_ALPHA -> WebGL2.ONE_MINUS_SRC_ALPHA;
            case DST_ALPHA -> WebGL2.DST_ALPHA;
            case ONE_MINUS_DST_ALPHA -> WebGL2.ONE_MINUS_DST_ALPHA;
        };
    }

    @Override
    public int createBuffer() {
        return store(gl.createBuffer());
    }

    @Override
    public void deleteBuffer(int buffer) {
        gl.deleteBuffer(lookup(buffer, "buffer"));
        handles.set(buffer, null);
    }

    @Override
    public void bindArrayBuffer(int buffer) {
        gl.bindBuffer(WebGL2.ARRAY_BUFFER, buffer == NO_BUFFER ? null : lookup(buffer, "buffer"));
    }

    @Override
    public void bindElementBuffer(int buffer) {
        gl.bindBuffer(WebGL2.ELEMENT_ARRAY_BUFFER,
                buffer == NO_BUFFER ? null : lookup(buffer, "buffer"));
    }

    @Override
    public void bufferData(float[] data, int length, BufferUsage usage) {
        if (length > data.length) {
            throw new IllegalArgumentException("length " + length + " exceeds array " + data.length);
        }
        scratch = Float32Array.copyFromJavaArray(data);
        gl.bufferData(WebGL2.ARRAY_BUFFER, scratch, usageConstant(usage), 0, length);
    }

    @Override
    public void elementData(int[] data, int length, BufferUsage usage) {
        if (length > data.length) {
            throw new IllegalArgumentException("length " + length + " exceeds array " + data.length);
        }
        gl.bufferData(WebGL2.ELEMENT_ARRAY_BUFFER, Uint32Array.copyFromJavaArray(data),
                usageConstant(usage), 0, length);
    }

    private static int usageConstant(BufferUsage usage) {
        return switch (usage) {
            case STATIC -> WebGL2.STATIC_DRAW;
            case DYNAMIC -> WebGL2.DYNAMIC_DRAW;
            case STREAM -> WebGL2.STREAM_DRAW;
        };
    }

    @Override
    public int createProgram(String vertexSource, String fragmentSource) {
        JSObject vertex = compile(WebGL2.VERTEX_SHADER, vertexSource, "vertex");
        JSObject fragment = compile(WebGL2.FRAGMENT_SHADER, fragmentSource, "fragment");
        JSObject program = gl.createProgram();
        gl.attachShader(program, vertex);
        gl.attachShader(program, fragment);
        gl.linkProgram(program);
        // shaders are reference-counted by the program, so they can go as soon as it links
        gl.deleteShader(vertex);
        gl.deleteShader(fragment);
        if (!gl.getProgramParameter(program, WebGL2.LINK_STATUS)) {
            String log = gl.getProgramInfoLog(program);
            gl.deleteProgram(program);
            throw new ShaderException("program failed to link: " + log);
        }
        return store(program);
    }

    private JSObject compile(int type, String source, String stage) {
        JSObject shader = gl.createShader(type);
        gl.shaderSource(shader, source);
        gl.compileShader(shader);
        if (!gl.getShaderParameter(shader, WebGL2.COMPILE_STATUS)) {
            String log = gl.getShaderInfoLog(shader);
            gl.deleteShader(shader);
            throw new ShaderException(stage + " shader failed to compile: " + log);
        }
        return shader;
    }

    @Override
    public void deleteProgram(int program) {
        gl.deleteProgram(lookup(program, "program"));
        handles.set(program, null);
    }

    @Override
    public void useProgram(int program) {
        gl.useProgram(program == NO_PROGRAM ? null : lookup(program, "program"));
    }

    @Override
    public int uniformLocation(int program, String name) {
        JSObject location = gl.getUniformLocation(lookup(program, "program"), name);
        if (location == null) {
            return -1;
        }
        uniforms.add(location);
        return uniforms.size() - 1;
    }

    private JSObject uniform(int location) {
        if (location <= 0 || location >= uniforms.size()) {
            throw new IllegalArgumentException("not a uniform location: " + location);
        }
        return uniforms.get(location);
    }

    @Override
    public void uniformMatrix4(int location, float[] matrix) {
        if (matrix.length < 16) {
            throw new IllegalArgumentException("a 4x4 matrix needs 16 floats, got " + matrix.length);
        }
        gl.uniformMatrix4fv(uniform(location), false, Float32Array.copyFromJavaArray(matrix));
    }

    @Override
    public void uniform1i(int location, int value) {
        gl.uniform1i(uniform(location), value);
    }

    @Override
    public void uniform1f(int location, float value) {
        gl.uniform1f(uniform(location), value);
    }

    @Override
    public void uniform4f(int location, float x, float y, float z, float w) {
        gl.uniform4f(uniform(location), x, y, z, w);
    }

    @Override
    public int attributeLocation(int program, String name) {
        return gl.getAttribLocation(lookup(program, "program"), name);
    }

    @Override
    public void enableAttribute(int location) {
        gl.enableVertexAttribArray(location);
    }

    @Override
    public void disableAttribute(int location) {
        gl.disableVertexAttribArray(location);
    }

    @Override
    public void attributePointer(int location, int components, int strideInFloats, int offsetInFloats) {
        gl.vertexAttribPointer(location, components, WebGL2.FLOAT, false,
                strideInFloats * Float.BYTES, offsetInFloats * Float.BYTES);
    }

    @Override
    public int createTexture() {
        return store(gl.createTexture());
    }

    @Override
    public void deleteTexture(int texture) {
        gl.deleteTexture(lookup(texture, "texture"));
        handles.set(texture, null);
    }

    @Override
    public void bindTexture(int unit, int texture) {
        gl.activeTexture(WebGL2.TEXTURE0 + unit);
        gl.bindTexture(WebGL2.TEXTURE_2D, texture == NO_TEXTURE ? null : lookup(texture, "texture"));
    }

    @Override
    public void textureImage(int width, int height, int[] pixels) {
        if (pixels.length < width * height) {
            throw new IllegalArgumentException(
                    "need " + (width * height) + " pixels, got " + pixels.length);
        }
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0, out = 0; i < width * height; i++) {
            int argb = pixels[i];
            rgba[out++] = (byte) (argb >> 16);
            rgba[out++] = (byte) (argb >> 8);
            rgba[out++] = (byte) argb;
            rgba[out++] = (byte) (argb >>> 24);
        }
        Uint8Array data = Uint8Array.create(Int8Array.copyFromJavaArray(rgba).getBuffer());
        gl.pixelStorei(WebGL2.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 0);
        gl.texImage2D(WebGL2.TEXTURE_2D, 0, WebGL2.RGBA, width, height, 0,
                WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, data);
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_WRAP_S, WebGL2.CLAMP_TO_EDGE);
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_WRAP_T, WebGL2.CLAMP_TO_EDGE);
        setTextureFilter(false);
    }

    @Override
    public void setTextureFilter(boolean smooth) {
        int filter = smooth ? WebGL2.LINEAR : WebGL2.NEAREST;
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_MIN_FILTER, filter);
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_MAG_FILTER, filter);
    }

    @Override
    public void drawArrays(Primitive primitive, int first, int count) {
        gl.drawArrays(mode(primitive), first, count);
    }

    @Override
    public void drawElements(Primitive primitive, int count) {
        gl.drawElements(mode(primitive), count, WebGL2.UNSIGNED_INT, 0);
    }

    private static int mode(Primitive primitive) {
        return switch (primitive) {
            case POINTS -> WebGL2.POINTS;
            case LINES -> WebGL2.LINES;
            case LINE_STRIP -> WebGL2.LINE_STRIP;
            case LINE_LOOP -> WebGL2.LINE_LOOP;
            case TRIANGLES -> WebGL2.TRIANGLES;
            case TRIANGLE_STRIP -> WebGL2.TRIANGLE_STRIP;
            case TRIANGLE_FAN -> WebGL2.TRIANGLE_FAN;
        };
    }

    @Override
    public int width() {
        return gl.getDrawingBufferWidth();
    }

    @Override
    public int height() {
        return gl.getDrawingBufferHeight();
    }

    @Override
    public String takeError() {
        String pending = pendingError;
        pendingError = null;
        if (pending != null) {
            return pending;
        }
        int code = gl.getError();
        return code == 0 ? null : "gl error 0x" + Integer.toHexString(code);
    }
}
