package dev.flagon.platform.browser.graphics;

import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.CullFace;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsError;
import dev.flagon.platform.graphics.GraphicsInfo;
import dev.flagon.platform.graphics.GraphicsLimit;
import dev.flagon.platform.graphics.IndexType;
import dev.flagon.platform.graphics.Primitive;
import dev.flagon.platform.graphics.ShaderException;
import dev.flagon.platform.graphics.ShaderStage;
import dev.flagon.platform.graphics.TextureFilter;
import dev.flagon.platform.graphics.TextureWrap;
import dev.flagon.platform.graphics.Winding;
import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.browser.js.JsArrays;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint8Array;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** {@link GraphicsDevice} on a WebGL2 context. */
public final class WebGLGraphicsDevice implements GraphicsDevice {

    private final WebGL2 gl;

    private final GlNames handles = new GlNames();
    private final GlNames uniforms = new GlNames();
    private final Map<String, Integer> uniformNames = new HashMap<>();

    // gl gives a texture its parameters when the name is first bound, not on every upload
    private final Set<Integer> defaulted = new HashSet<>();

    public WebGLGraphicsDevice(WebGL2 gl) {
        this.gl = gl;
        // webgl2 core profile requires a bound vertex array object before any attribute is set
        gl.bindVertexArray(gl.createVertexArray());
    }

    private int store(JSObject object) {
        return handles.add(object);
    }

    private JSObject lookup(int name, String kind) {
        JSObject object = handles.get(name);
        if (object == null) {
            throw new IllegalArgumentException("not a live " + kind + ": " + name);
        }
        return object;
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
    public void setCulledFaces(CullFace faces) {
        gl.cullFace(switch (faces) {
            case FRONT -> WebGL2.FRONT;
            case BACK -> WebGL2.BACK;
            case FRONT_AND_BACK -> WebGL2.FRONT_AND_BACK;
        });
    }

    @Override
    public void setFrontFace(Winding winding) {
        gl.frontFace(winding == Winding.CLOCKWISE ? WebGL2.CW : WebGL2.CCW);
    }

    @Override
    public void setScissorTest(boolean enabled) {
        capability(WebGL2.SCISSOR_TEST, enabled);
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        gl.scissor(x, y, width, height);
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
        handles.remove(buffer);
    }

    @Override
    public void bindBuffer(BufferTarget target, int buffer) {
        gl.bindBuffer(targetConstant(target), buffer == NO_BUFFER ? null : lookup(buffer, "buffer"));
    }

    @Override
    public void bufferData(BufferTarget target, int sizeInBytes, BufferUsage usage) {
        gl.bufferData(targetConstant(target), sizeInBytes, usageConstant(usage));
    }

    // only the prefix the caller means is handed over, so growable arrays cost nothing for the
    // slack they carry; JsArrays decides whether that is a view or a copy
    @Override
    public void bufferData(BufferTarget target, byte[] data, int length, BufferUsage usage) {
        gl.bufferData(targetConstant(target), JsArrays.bytes(data, length),
                usageConstant(usage), 0, length);
    }

    @Override
    public void bufferData(BufferTarget target, float[] data, int length, BufferUsage usage) {
        gl.bufferData(targetConstant(target), JsArrays.floats(data, length),
                usageConstant(usage), 0, length);
    }

    @Override
    public void bufferData(BufferTarget target, int[] data, int length, BufferUsage usage) {
        gl.bufferData(targetConstant(target), JsArrays.unsignedInts(data, length),
                usageConstant(usage), 0, length);
    }

    @Override
    public void bufferSubData(BufferTarget target, int offsetInBytes, byte[] data, int length) {
        gl.bufferSubData(targetConstant(target), offsetInBytes, JsArrays.bytes(data, length),
                0, length);
    }

    private static int targetConstant(BufferTarget target) {
        return switch (target) {
            case ARRAY -> WebGL2.ARRAY_BUFFER;
            case ELEMENT -> WebGL2.ELEMENT_ARRAY_BUFFER;
        };
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
    public int createShader(ShaderStage stage) {
        return store(gl.createShader(stage == ShaderStage.VERTEX
                ? WebGL2.VERTEX_SHADER
                : WebGL2.FRAGMENT_SHADER));
    }

    @Override
    public void shaderSource(int shader, String source) {
        gl.shaderSource(lookup(shader, "shader"), source);
    }

    @Override
    public void compileShader(int shader) {
        gl.compileShader(lookup(shader, "shader"));
    }

    @Override
    public boolean isShaderCompiled(int shader) {
        return gl.getShaderParameter(lookup(shader, "shader"), WebGL2.COMPILE_STATUS);
    }

    @Override
    public String shaderInfoLog(int shader) {
        String log = gl.getShaderInfoLog(lookup(shader, "shader"));
        return log == null ? "" : log;
    }

    @Override
    public void deleteShader(int shader) {
        gl.deleteShader(lookup(shader, "shader"));
        handles.remove(shader);
    }

    @Override
    public boolean isShader(int shader) {
        return live(shader) && gl.isShader(handles.get(shader));
    }

    @Override
    public int createProgram() {
        return store(gl.createProgram());
    }

    @Override
    public void attachShader(int program, int shader) {
        gl.attachShader(lookup(program, "program"), lookup(shader, "shader"));
    }

    @Override
    public void detachShader(int program, int shader) {
        gl.detachShader(lookup(program, "program"), lookup(shader, "shader"));
    }

    @Override
    public void bindAttributeLocation(int program, int location, String name) {
        gl.bindAttribLocation(lookup(program, "program"), location, name);
    }

    @Override
    public void linkProgram(int program) {
        gl.linkProgram(lookup(program, "program"));
        // locations are assigned by the linker, so anything resolved before it is stale
        forgetUniforms(program);
    }

    @Override
    public boolean isProgramLinked(int program) {
        return gl.getProgramParameter(lookup(program, "program"), WebGL2.LINK_STATUS);
    }

    @Override
    public void validateProgram(int program) {
        gl.validateProgram(lookup(program, "program"));
    }

    @Override
    public boolean isProgramValid(int program) {
        return gl.getProgramParameter(lookup(program, "program"), WebGL2.VALIDATE_STATUS);
    }

    @Override
    public String programInfoLog(int program) {
        String log = gl.getProgramInfoLog(lookup(program, "program"));
        return log == null ? "" : log;
    }

    @Override
    public boolean isProgram(int program) {
        return live(program) && gl.isProgram(handles.get(program));
    }

    private boolean live(int name) {
        return handles.get(name) != null;
    }

    @Override
    public void deleteProgram(int program) {
        gl.deleteProgram(lookup(program, "program"));
        handles.remove(program);
        forgetUniforms(program);
    }

    // the location objects go with the names, or a program's uniforms would be held for the life
    // of the page by a table nothing can reach them through any more
    private void forgetUniforms(int program) {
        String prefix = program + "/";
        Iterator<Map.Entry<String, Integer>> known = uniformNames.entrySet().iterator();
        while (known.hasNext()) {
            Map.Entry<String, Integer> entry = known.next();
            if (entry.getKey().startsWith(prefix)) {
                uniforms.remove(entry.getValue());
                known.remove();
            }
        }
    }

    @Override
    public void useProgram(int program) {
        gl.useProgram(program == NO_PROGRAM ? null : lookup(program, "program"));
    }

    // one int per (program, name), so resolving the same uniform every frame does not grow the
    // table; webgl location objects are opaque and cannot be compared, hence the name key
    @Override
    public int uniformLocation(int program, String name) {
        String key = program + "/" + name;
        Integer known = uniformNames.get(key);
        if (known != null) {
            return known;
        }
        JSObject location = gl.getUniformLocation(lookup(program, "program"), name);
        if (location == null) {
            return -1;
        }
        int assigned = uniforms.add(location);
        uniformNames.put(key, assigned);
        return assigned;
    }

    /**
     * @return the location object, or null for a uniform the shader does not use — writing to
     *         one of those is a no-op in gl, which is what makes it safe to resolve a uniform
     *         once and set it unconditionally
     */
    private JSObject uniform(int location) {
        if (location == NO_UNIFORM) {
            return null;
        }
        JSObject found = uniforms.get(location);
        if (found == null) {
            throw new IllegalArgumentException("not a uniform location: " + location);
        }
        return found;
    }

    @Override
    public void uniformMatrix4(int location, float[] matrix) {
        if (matrix.length < 16) {
            throw new IllegalArgumentException("a 4x4 matrix needs 16 floats, got " + matrix.length);
        }
        uniformMatrices(location, 4, false, matrix, 1);
    }

    @Override
    public void uniform1i(int location, int value) {
        if (location == NO_UNIFORM) {
            return;
        }
        gl.uniform1i(uniform(location), value);
    }

    @Override
    public void uniform1f(int location, float value) {
        if (location == NO_UNIFORM) {
            return;
        }
        gl.uniform1f(uniform(location), value);
    }

    @Override
    public void uniform2f(int location, float x, float y) {
        if (location == NO_UNIFORM) {
            return;
        }
        gl.uniform2f(uniform(location), x, y);
    }

    @Override
    public void uniform3f(int location, float x, float y, float z) {
        if (location == NO_UNIFORM) {
            return;
        }
        gl.uniform3f(uniform(location), x, y, z);
    }

    @Override
    public void uniform4f(int location, float x, float y, float z, float w) {
        if (location == NO_UNIFORM) {
            return;
        }
        gl.uniform4f(uniform(location), x, y, z, w);
    }

    @Override
    public void uniform2i(int location, int x, int y) {
        if (location == NO_UNIFORM) {
            return;
        }
        gl.uniform2i(uniform(location), x, y);
    }

    @Override
    public void uniform3i(int location, int x, int y, int z) {
        if (location == NO_UNIFORM) {
            return;
        }
        gl.uniform3i(uniform(location), x, y, z);
    }

    @Override
    public void uniform4i(int location, int x, int y, int z, int w) {
        if (location == NO_UNIFORM) {
            return;
        }
        gl.uniform4i(uniform(location), x, y, z, w);
    }

    @Override
    public void uniformFloats(int location, int components, float[] values, int count) {
        if (location == NO_UNIFORM) {
            return;
        }
        Float32Array data = JsArrays.floats(values, components * count);
        JSObject at = uniform(location);
        switch (components) {
            case 1 -> gl.uniform1fv(at, data);
            case 2 -> gl.uniform2fv(at, data);
            case 3 -> gl.uniform3fv(at, data);
            case 4 -> gl.uniform4fv(at, data);
            default -> throw new IllegalArgumentException(
                    "a uniform vector has 1 to 4 components, got " + components);
        }
    }

    @Override
    public void uniformInts(int location, int components, int[] values, int count) {
        if (location == NO_UNIFORM) {
            return;
        }
        Int32Array data = JsArrays.ints(values, components * count);
        JSObject at = uniform(location);
        switch (components) {
            case 1 -> gl.uniform1iv(at, data);
            case 2 -> gl.uniform2iv(at, data);
            case 3 -> gl.uniform3iv(at, data);
            case 4 -> gl.uniform4iv(at, data);
            default -> throw new IllegalArgumentException(
                    "a uniform vector has 1 to 4 components, got " + components);
        }
    }

    @Override
    public void uniformMatrices(int location, int size, boolean transpose, float[] values,
                                int count) {
        if (location == NO_UNIFORM) {
            return;
        }
        Float32Array data = JsArrays.floats(values, size * size * count);
        JSObject at = uniform(location);
        switch (size) {
            case 2 -> gl.uniformMatrix2fv(at, transpose, data);
            case 3 -> gl.uniformMatrix3fv(at, transpose, data);
            case 4 -> gl.uniformMatrix4fv(at, transpose, data);
            default -> throw new IllegalArgumentException(
                    "a uniform matrix is 2x2 to 4x4, got " + size + "x" + size);
        }
    }

    @Override
    public int attributeLocation(int program, String name) {
        return gl.getAttribLocation(lookup(program, "program"), name);
    }

    @Override
    public int maxAttributes() {
        return gl.getParameter(WebGL2.MAX_VERTEX_ATTRIBS);
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
    public void attributePointer(int location, int components, AttributeType type,
                                 boolean normalized, int strideInBytes, int offsetInBytes) {
        requireAligned(type, strideInBytes, "stride");
        requireAligned(type, offsetInBytes, "offset");
        gl.vertexAttribPointer(location, components, attributeType(type), normalized,
                strideInBytes, offsetInBytes);
    }

    /**
     * webgl requires an attribute's offset and stride to be a multiple of its component size,
     * where desktop gl does not. an interleaved format that pads to an odd width is legal on the
     * desktop and draws nothing here, so it is named rather than left to a console warning.
     */
    private static void requireAligned(AttributeType type, int value, String what) {
        int size = type.sizeInBytes();
        if (value % size != 0) {
            throw new UnsupportedCapabilityException(
                    "vertex attribute " + what + " " + value + " is not a multiple of " + size
                            + ", the size of a " + type + ". webgl requires that alignment; pad "
                            + "the vertex format so every attribute starts on its own boundary");
        }
    }

    @Override
    public void constantAttribute(int location, float x, float y, float z, float w) {
        gl.vertexAttrib4f(location, x, y, z, w);
    }

    private static int attributeType(AttributeType type) {
        return switch (type) {
            case BYTE -> WebGL2.BYTE;
            case UNSIGNED_BYTE -> WebGL2.UNSIGNED_BYTE;
            case SHORT -> WebGL2.SHORT;
            case UNSIGNED_SHORT -> WebGL2.UNSIGNED_SHORT;
            case INT -> WebGL2.INT;
            case UNSIGNED_INT -> WebGL2.UNSIGNED_INT;
            case FLOAT -> WebGL2.FLOAT;
        };
    }

    @Override
    public int createTexture() {
        return store(gl.createTexture());
    }

    @Override
    public void deleteTexture(int texture) {
        gl.deleteTexture(lookup(texture, "texture"));
        handles.remove(texture);
        defaulted.remove(texture);
    }

    @Override
    public void bindTexture(int unit, int texture) {
        gl.activeTexture(WebGL2.TEXTURE0 + unit);
        gl.bindTexture(WebGL2.TEXTURE_2D, texture == NO_TEXTURE ? null : lookup(texture, "texture"));
        if (texture != NO_TEXTURE && defaulted.add(texture)) {
            setTextureWrapS(TextureWrap.CLAMP_TO_EDGE);
            setTextureWrapT(TextureWrap.CLAMP_TO_EDGE);
            setTextureMinFilter(TextureFilter.NEAREST);
            setTextureMagFilter(TextureFilter.NEAREST);
        }
    }

    @Override
    public void textureImage(int width, int height, byte[] rgba) {
        Uint8Array data = pixels(width, height, rgba);
        gl.pixelStorei(WebGL2.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 0);
        gl.texImage2D(WebGL2.TEXTURE_2D, 0, WebGL2.RGBA, width, height, 0,
                WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, data);
    }

    @Override
    public void textureSubImage(int x, int y, int width, int height, byte[] rgba) {
        Uint8Array data = pixels(width, height, rgba);
        gl.pixelStorei(WebGL2.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 0);
        gl.texSubImage2D(WebGL2.TEXTURE_2D, 0, x, y, width, height,
                WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, data);
    }

    private static Uint8Array pixels(int width, int height, byte[] rgba) {
        int needed = width * height * 4;
        if (rgba.length < needed) {
            throw new IllegalArgumentException(
                    "need " + needed + " bytes, got " + rgba.length);
        }
        return Uint8Array.create(Int8Array.copyFromJavaArray(rgba).getBuffer());
    }

    @Override
    public void setTextureMinFilter(TextureFilter filter) {
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_MIN_FILTER, filter(filter));
    }

    @Override
    public void setTextureMagFilter(TextureFilter filter) {
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_MAG_FILTER, filter(filter));
    }

    @Override
    public void setTextureWrapS(TextureWrap wrap) {
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_WRAP_S, wrap(wrap));
    }

    @Override
    public void setTextureWrapT(TextureWrap wrap) {
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_WRAP_T, wrap(wrap));
    }

    private static int filter(TextureFilter filter) {
        return switch (filter) {
            case NEAREST -> WebGL2.NEAREST;
            case LINEAR -> WebGL2.LINEAR;
        };
    }

    private static int wrap(TextureWrap wrap) {
        return switch (wrap) {
            case REPEAT -> WebGL2.REPEAT;
            case CLAMP_TO_EDGE -> WebGL2.CLAMP_TO_EDGE;
            case MIRRORED_REPEAT -> WebGL2.MIRRORED_REPEAT;
        };
    }

    @Override
    public void drawArrays(Primitive primitive, int first, int count) {
        gl.drawArrays(mode(primitive), first, count);
    }

    @Override
    public void drawElements(Primitive primitive, int count, IndexType type, int offsetInBytes) {
        gl.drawElements(mode(primitive), count, indexType(type), offsetInBytes);
    }

    private static int indexType(IndexType type) {
        return switch (type) {
            case UNSIGNED_BYTE -> WebGL2.UNSIGNED_BYTE;
            case UNSIGNED_SHORT -> WebGL2.UNSIGNED_SHORT;
            case UNSIGNED_INT -> WebGL2.UNSIGNED_INT;
        };
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
    public GraphicsError takeError() {
        return switch (gl.getError()) {
            case 0 -> GraphicsError.NONE;
            case WebGL2.INVALID_ENUM -> GraphicsError.INVALID_ENUM;
            case WebGL2.INVALID_VALUE -> GraphicsError.INVALID_VALUE;
            case WebGL2.INVALID_OPERATION -> GraphicsError.INVALID_OPERATION;
            case WebGL2.INVALID_FRAMEBUFFER_OPERATION -> GraphicsError.INVALID_FRAMEBUFFER_OPERATION;
            case WebGL2.OUT_OF_MEMORY -> GraphicsError.OUT_OF_MEMORY;
            default -> GraphicsError.CONTEXT_LOST;
        };
    }

    @Override
    public String information(GraphicsInfo information) {
        if (information == GraphicsInfo.EXTENSIONS) {
            return extensions();
        }
        String said = gl.getParameterString(switch (information) {
            case VENDOR -> WebGL2.VENDOR;
            case RENDERER -> WebGL2.RENDERER;
            case VERSION -> WebGL2.VERSION;
            case SHADING_LANGUAGE_VERSION -> WebGL2.SHADING_LANGUAGE_VERSION;
            case EXTENSIONS -> WebGL2.VENDOR;
        });
        return said == null ? "" : said;
    }

    private String extensions() {
        JSArray<JSString> supported = gl.getSupportedExtensions();
        if (supported == null) {
            return "";
        }
        StringBuilder joined = new StringBuilder();
        for (int index = 0; index < supported.getLength(); index++) {
            if (index > 0) {
                joined.append(' ');
            }
            joined.append(supported.get(index).stringValue());
        }
        return joined.toString();
    }

    @Override
    public int limit(GraphicsLimit limit) {
        return gl.getParameter(switch (limit) {
            case MAX_TEXTURE_SIZE -> WebGL2.MAX_TEXTURE_SIZE;
            case MAX_TEXTURE_UNITS -> WebGL2.MAX_TEXTURE_IMAGE_UNITS;
            case MAX_VERTEX_ATTRIBUTES -> WebGL2.MAX_VERTEX_ATTRIBS;
        });
    }
}
