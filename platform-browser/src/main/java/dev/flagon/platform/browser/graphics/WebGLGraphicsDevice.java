package dev.flagon.platform.browser.graphics;

import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BlendEquation;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.BufferTarget;
import dev.flagon.platform.graphics.BufferUsage;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.CullFace;
import dev.flagon.platform.graphics.FramebufferAttachment;
import dev.flagon.platform.graphics.FramebufferStatus;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsError;
import dev.flagon.platform.graphics.GraphicsInfo;
import dev.flagon.platform.graphics.GraphicsLimit;
import dev.flagon.platform.graphics.IndexType;
import dev.flagon.platform.graphics.Primitive;
import dev.flagon.platform.graphics.RenderbufferFormat;
import dev.flagon.platform.graphics.ShaderException;
import dev.flagon.platform.graphics.ShaderStage;
import dev.flagon.platform.graphics.StencilOp;
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
import org.teavm.jso.typedarrays.Uint32Array;
import org.teavm.jso.typedarrays.Uint8Array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@link GraphicsDevice} on WebGL2 context */
public final class WebGLGraphicsDevice implements GraphicsDevice {

    private final WebGL2 gl;

    private final GlNames handles = new GlNames();
    private final GlNames uniforms = new GlNames();
    private final List<Map<String, Integer>> uniformNames = new ArrayList<>();
    private final UniformCache cache = new UniformCache();

    private final JSObject defaultVertexArray;

    private final BitSet defaulted = new BitSet();

    private static final int POINTER_FIELDS = 6;

    private final boolean[] capabilityOn = new boolean[6];
    private final boolean[] capabilityKnown = new boolean[6];

    private final int[] boundTexture = new int[32];
    private int activeUnit = -1;

    private int boundArrayBuffer = NO_BUFFER;
    private int boundFramebuffer = NO_FRAMEBUFFER;
    private int boundProgram = NO_PROGRAM;

    private final List<Attributes> arrays = new ArrayList<>();
    private int boundArray = NO_VERTEX_ARRAY;
    private float[] constants = new float[0];
    private Uint32Array swizzled;
    private final BitSet constantsKnown = new BitSet();

    private int viewportX;
    private int viewportY;
    private int viewportWidth = -1;
    private int viewportHeight = -1;
    private int scissorX;
    private int scissorY;
    private int scissorWidth = -1;
    private int scissorHeight = -1;
    private int blendEquationColor = WebGL2.FUNC_ADD;
    private int blendEquationAlpha = WebGL2.FUNC_ADD;
    private final float[] blendColor = new float[4];

    private boolean depthWrite = true;
    private int depthFunc = WebGL2.LESS;
    private int blendSourceColor = WebGL2.ONE;
    private int blendDestinationColor = WebGL2.ZERO;
    private int blendSourceAlpha = WebGL2.ONE;
    private int blendDestinationAlpha = WebGL2.ZERO;
    private int culledFaces = WebGL2.BACK;
    private int frontFace = WebGL2.CCW;
    private boolean redMask = true;
    private boolean greenMask = true;
    private boolean blueMask = true;
    private boolean alphaMask = true;

    private static final class Attributes {

        private int elementBuffer = NO_BUFFER;
        private final BitSet enabled = new BitSet();
        private final BitSet described = new BitSet();
        private int[] pointers = new int[0];
    }

    public WebGLGraphicsDevice(WebGL2 gl) {
        this.gl = gl;
        this.defaultVertexArray = gl.createVertexArray();

        gl.bindVertexArray(defaultVertexArray);
        gl.pixelStorei(WebGL2.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 0);

    }

    private Attributes attributes() {
        while (arrays.size() <= boundArray) {
            arrays.add(null);
        }
        Attributes held = arrays.get(boundArray);
        if (held == null) {
            held = new Attributes();
            arrays.set(boundArray, held);
        }
        return held;
    }

    @Override
    public int createVertexArray() {
        return store(gl.createVertexArray());
    }

    @Override
    public void bindVertexArray(int array) {
        gl.bindVertexArray(array == NO_VERTEX_ARRAY ? defaultVertexArray
                : lookup(array, "vertex array"));
        boundArray = array;
    }

    @Override
    public void deleteVertexArray(int array) {
        if (array == NO_VERTEX_ARRAY) {
            return;
        }
        gl.deleteVertexArray(lookup(array, "vertex array"));
        handles.remove(array);
        if (array < arrays.size()) {
            arrays.set(array, null);
        }
        if (boundArray == array) {
            boundArray = NO_VERTEX_ARRAY;
        }
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

    private JSObject liveTexture(int name) {
        JSObject texture = handles.get(name);
        if (texture == null) {
            texture = gl.createTexture();
            handles.put(name, texture);
        }
        return texture;
    }

    @Override
    public void viewport(int x, int y, int width, int height) {
        if (viewportX == x && viewportY == y && viewportWidth == width
                && viewportHeight == height) {
            return;
        }
        viewportX = x;
        viewportY = y;
        viewportWidth = width;
        viewportHeight = height;
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
        capability(0, WebGL2.DEPTH_TEST, enabled);
    }

    @Override
    public void setDepthWrite(boolean enabled) {
        if (depthWrite == enabled) {
            return;
        }
        depthWrite = enabled;
        gl.depthMask(enabled);
    }

    @Override
    public void setDepthFunc(CompareFunc func) {
        int wanted = compare(func);
        if (depthFunc == wanted) {
            return;
        }
        depthFunc = wanted;
        gl.depthFunc(wanted);
    }

    private static int compare(CompareFunc func) {
        return switch (func) {
            case NEVER -> WebGL2.NEVER;
            case LESS -> WebGL2.LESS;
            case EQUAL -> WebGL2.EQUAL;
            case LESS_OR_EQUAL -> WebGL2.LEQUAL;
            case GREATER -> WebGL2.GREATER;
            case NOT_EQUAL -> WebGL2.NOTEQUAL;
            case GREATER_OR_EQUAL -> WebGL2.GEQUAL;
            case ALWAYS -> WebGL2.ALWAYS;
        };
    }

    @Override
    public void setBlend(boolean enabled) {
        capability(1, WebGL2.BLEND, enabled);
    }

    @Override
    public void setBlendFunc(BlendFactor source, BlendFactor destination) {
        int wantedSource = blendFactor(source);
        int wantedDestination = blendFactor(destination);
        if (blendSourceColor == wantedSource && blendDestinationColor == wantedDestination
                && blendSourceAlpha == wantedSource && blendDestinationAlpha == wantedDestination) {
            return;
        }
        blendSourceColor = wantedSource;
        blendDestinationColor = wantedDestination;
        blendSourceAlpha = wantedSource;
        blendDestinationAlpha = wantedDestination;
        gl.blendFunc(wantedSource, wantedDestination);
    }

    @Override
    public void setBlendFuncSeparate(BlendFactor sourceColor, BlendFactor destinationColor,
                                     BlendFactor sourceAlpha, BlendFactor destinationAlpha) {
        int wantedSourceColor = blendFactor(sourceColor);
        int wantedDestinationColor = blendFactor(destinationColor);
        int wantedSourceAlpha = blendFactor(sourceAlpha);
        int wantedDestinationAlpha = blendFactor(destinationAlpha);
        if (blendSourceColor == wantedSourceColor && blendDestinationColor == wantedDestinationColor
                && blendSourceAlpha == wantedSourceAlpha
                && blendDestinationAlpha == wantedDestinationAlpha) {
            return;
        }
        blendSourceColor = wantedSourceColor;
        blendDestinationColor = wantedDestinationColor;
        blendSourceAlpha = wantedSourceAlpha;
        blendDestinationAlpha = wantedDestinationAlpha;
        gl.blendFuncSeparate(wantedSourceColor, wantedDestinationColor,
                wantedSourceAlpha, wantedDestinationAlpha);
    }

    @Override
    public void setBlendEquation(BlendEquation equation) {
        setBlendEquationSeparate(equation, equation);
    }

    @Override
    public void setBlendEquationSeparate(BlendEquation color, BlendEquation alpha) {
        int wantedColor = blendEquation(color);
        int wantedAlpha = blendEquation(alpha);
        if (blendEquationColor == wantedColor && blendEquationAlpha == wantedAlpha) {
            return;
        }
        blendEquationColor = wantedColor;
        blendEquationAlpha = wantedAlpha;
        gl.blendEquationSeparate(wantedColor, wantedAlpha);
    }

    @Override
    public void setBlendColor(float red, float green, float blue, float alpha) {
        if (blendColor[0] == red && blendColor[1] == green && blendColor[2] == blue
                && blendColor[3] == alpha) {
            return;
        }
        blendColor[0] = red;
        blendColor[1] = green;
        blendColor[2] = blue;
        blendColor[3] = alpha;
        gl.blendColor(red, green, blue, alpha);
    }

    @Override
    public void setCullFace(boolean enabled) {
        capability(2, WebGL2.CULL_FACE, enabled);
    }

    @Override
    public void setCulledFaces(CullFace faces) {
        int wanted = switch (faces) {
            case FRONT -> WebGL2.FRONT;
            case BACK -> WebGL2.BACK;
            case FRONT_AND_BACK -> WebGL2.FRONT_AND_BACK;
        };
        if (culledFaces == wanted) {
            return;
        }
        culledFaces = wanted;
        gl.cullFace(wanted);
    }

    @Override
    public void setFrontFace(Winding winding) {
        int wanted = winding == Winding.CLOCKWISE ? WebGL2.CW : WebGL2.CCW;
        if (frontFace == wanted) {
            return;
        }
        frontFace = wanted;
        gl.frontFace(wanted);
    }

    @Override
    public void setScissorTest(boolean enabled) {
        capability(3, WebGL2.SCISSOR_TEST, enabled);
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        if (scissorX == x && scissorY == y && scissorWidth == width && scissorHeight == height) {
            return;
        }
        scissorX = x;
        scissorY = y;
        scissorWidth = width;
        scissorHeight = height;
        gl.scissor(x, y, width, height);
    }

    @Override
    public void setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        if (redMask == red && greenMask == green && blueMask == blue && alphaMask == alpha) {
            return;
        }
        redMask = red;
        greenMask = green;
        blueMask = blue;
        alphaMask = alpha;
        gl.colorMask(red, green, blue, alpha);
    }

    @Override
    public void setStencilTest(boolean enabled) {
        capability(4, WebGL2.STENCIL_TEST, enabled);
    }

    @Override
    public void setStencilFunc(CompareFunc func, int reference, int mask) {
        gl.stencilFunc(compare(func), reference, mask);
    }

    @Override
    public void setStencilOp(StencilOp stencilFail, StencilOp depthFail, StencilOp pass) {
        gl.stencilOp(stencilOp(stencilFail), stencilOp(depthFail), stencilOp(pass));
    }

    @Override
    public void setStencilMask(int mask) {
        gl.stencilMask(mask);
    }

    @Override
    public void clearStencil(int value) {
        gl.clearStencil(value);
    }

    @Override
    public void setPolygonOffsetFill(boolean enabled) {
        capability(5, WebGL2.POLYGON_OFFSET_FILL, enabled);
    }

    @Override
    public void setPolygonOffset(float factor, float units) {
        gl.polygonOffset(factor, units);
    }

    @Override
    public void setLineWidth(float width) {
        gl.lineWidth(width);
    }

    private static int stencilOp(StencilOp op) {
        return switch (op) {
            case KEEP -> WebGL2.KEEP;
            case ZERO -> 0;
            case REPLACE -> WebGL2.REPLACE;
            case INCREMENT -> WebGL2.INCR;
            case INCREMENT_WRAP -> WebGL2.INCR_WRAP;
            case DECREMENT -> WebGL2.DECR;
            case DECREMENT_WRAP -> WebGL2.DECR_WRAP;
            case INVERT -> WebGL2.INVERT;
        };
    }

    private void capability(int slot, int capability, boolean enabled) {
        if (capabilityKnown[slot] && capabilityOn[slot] == enabled) {
            return;
        }
        capabilityKnown[slot] = true;
        capabilityOn[slot] = enabled;
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
            case CONSTANT_COLOR -> WebGL2.CONSTANT_COLOR;
            case ONE_MINUS_CONSTANT_COLOR -> WebGL2.ONE_MINUS_CONSTANT_COLOR;
            case CONSTANT_ALPHA -> WebGL2.CONSTANT_ALPHA;
            case ONE_MINUS_CONSTANT_ALPHA -> WebGL2.ONE_MINUS_CONSTANT_ALPHA;
            case SRC_ALPHA_SATURATE -> WebGL2.SRC_ALPHA_SATURATE;
        };
    }

    private static int blendEquation(BlendEquation equation) {
        return switch (equation) {
            case ADD -> WebGL2.FUNC_ADD;
            case SUBTRACT -> WebGL2.FUNC_SUBTRACT;
            case REVERSE_SUBTRACT -> WebGL2.FUNC_REVERSE_SUBTRACT;
            case MIN -> WebGL2.MIN;
            case MAX -> WebGL2.MAX;
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

        if (boundArrayBuffer == buffer) {
            boundArrayBuffer = NO_BUFFER;
        }

        for (Attributes held : arrays) {
            if (held == null) {
                continue;
            }
            if (held.elementBuffer == buffer) {
                held.elementBuffer = NO_BUFFER;
            }
            for (int slot = 0; slot * POINTER_FIELDS < held.pointers.length; slot++) {
                if (held.pointers[slot * POINTER_FIELDS] == buffer) {
                    held.described.clear(slot);
                }
            }
        }
    }

    @Override
    public void bindBuffer(BufferTarget target, int buffer) {
        if (target == BufferTarget.ARRAY) {
            if (buffer == boundArrayBuffer) {
                return;
            }
            boundArrayBuffer = buffer;
        } else {
            Attributes held = attributes();
            if (held.elementBuffer == buffer) {
                return;
            }
            held.elementBuffer = buffer;
        }
        gl.bindBuffer(targetConstant(target), buffer == NO_BUFFER ? null : lookup(buffer,
                "buffer"));
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

    @Override
    public void bufferSubData(BufferTarget target, int offsetInBytes, float[] data, int length) {
        gl.bufferSubData(targetConstant(target), offsetInBytes, JsArrays.floats(data, length),
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
        if (boundProgram == program) {
            boundProgram = NO_PROGRAM;
        }
    }

    private void forgetUniforms(int program) {
        if (program < 0 || program >= uniformNames.size()) {
            return;
        }
        Map<String, Integer> known = uniformNames.get(program);
        if (known == null) {
            return;
        }
        for (int location : known.values()) {
            if (location != NO_UNIFORM) {
                uniforms.remove(location);
                cache.forget(location);
            }
        }
        uniformNames.set(program, null);
    }

    private Map<String, Integer> uniformsOf(int program) {
        while (uniformNames.size() <= program) {
            uniformNames.add(null);
        }
        Map<String, Integer> known = uniformNames.get(program);
        if (known == null) {
            known = new HashMap<>();
            uniformNames.set(program, known);
        }
        return known;
    }

    @Override
    public void useProgram(int program) {
        if (boundProgram == program) {
            return;
        }
        boundProgram = program;
        gl.useProgram(program == NO_PROGRAM ? null : lookup(program, "program"));
    }

    @Override
    public int uniformLocation(int program, String name) {
        Map<String, Integer> known = uniformsOf(program);
        Integer found = known.get(name);
        if (found != null) {
            return found;
        }
        JSObject location = gl.getUniformLocation(lookup(program, "program"), name);
        int assigned = location == null ? NO_UNIFORM : uniforms.add(location);
        known.put(name, assigned);
        return assigned;
    }

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
            throw new IllegalArgumentException("a 4x4 matrix needs 16 floats, got "
                    + matrix.length);
        }
        uniformMatrices(location, 4, false, matrix, 1);
    }

    @Override
    public void uniform1i(int location, int value) {
        if (location == NO_UNIFORM) {
            return;
        }
        if (cache.changed(location, value)) {
            gl.uniform1i(uniform(location), value);
        }
    }

    @Override
    public void uniform1f(int location, float value) {
        if (location == NO_UNIFORM) {
            return;
        }
        if (cache.changed(location, Float.floatToRawIntBits(value))) {
            gl.uniform1f(uniform(location), value);
        }
    }

    @Override
    public void uniform2f(int location, float x, float y) {
        if (location == NO_UNIFORM) {
            return;
        }
        if (cache.changed(location, Float.floatToRawIntBits(x), Float.floatToRawIntBits(y))) {
            gl.uniform2f(uniform(location), x, y);
        }
    }

    @Override
    public void uniform3f(int location, float x, float y, float z) {
        if (location == NO_UNIFORM) {
            return;
        }
        if (cache.changed(location, Float.floatToRawIntBits(x), Float.floatToRawIntBits(y),
                Float.floatToRawIntBits(z))) {
            gl.uniform3f(uniform(location), x, y, z);
        }
    }

    @Override
    public void uniform4f(int location, float x, float y, float z, float w) {
        if (location == NO_UNIFORM) {
            return;
        }
        if (cache.changed(location, Float.floatToRawIntBits(x), Float.floatToRawIntBits(y),
                Float.floatToRawIntBits(z), Float.floatToRawIntBits(w))) {
            gl.uniform4f(uniform(location), x, y, z, w);
        }
    }

    @Override
    public void uniform2i(int location, int x, int y) {
        if (location == NO_UNIFORM) {
            return;
        }
        if (cache.changed(location, x, y)) {
            gl.uniform2i(uniform(location), x, y);
        }
    }

    @Override
    public void uniform3i(int location, int x, int y, int z) {
        if (location == NO_UNIFORM) {
            return;
        }
        if (cache.changed(location, x, y, z)) {
            gl.uniform3i(uniform(location), x, y, z);
        }
    }

    @Override
    public void uniform4i(int location, int x, int y, int z, int w) {
        if (location == NO_UNIFORM) {
            return;
        }
        if (cache.changed(location, x, y, z, w)) {
            gl.uniform4i(uniform(location), x, y, z, w);
        }
    }

    @Override
    public void uniformFloats(int location, int components, float[] values, int count) {
        if (location == NO_UNIFORM) {
            return;
        }
        requireComponents(components);
        if (!cache.changed(location, values, components * count)) {
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
        requireComponents(components);
        if (!cache.changed(location, values, components * count)) {
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
        requireMatrixSize(size);
        if (!cache.changed(location, values, size * size * count)) {
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

    private static void requireComponents(int components) {
        if (components < 1 || components > 4) {
            throw new IllegalArgumentException(
                    "a uniform vector has 1 to 4 components, got " + components);
        }
    }

    private static void requireMatrixSize(int size) {
        if (size < 2 || size > 4) {
            throw new IllegalArgumentException(
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
        Attributes held = attributes();
        if (held.enabled.get(location)) {
            return;
        }
        held.enabled.set(location);
        gl.enableVertexAttribArray(location);
    }

    @Override
    public void disableAttribute(int location) {
        Attributes held = attributes();
        if (!held.enabled.get(location)) {
            return;
        }
        held.enabled.clear(location);
        gl.disableVertexAttribArray(location);
    }

    @Override
    public void attributePointer(int location, int components, AttributeType type,
                                 boolean normalized, int strideInBytes, int offsetInBytes) {
        requireAligned(type, strideInBytes, "stride");
        requireAligned(type, offsetInBytes, "offset");
        if (described(location, components, attributeType(type), normalized ? 1 : 0,
                strideInBytes, offsetInBytes)) {
            return;
        }
        gl.vertexAttribPointer(location, components, attributeType(type), normalized,
                strideInBytes, offsetInBytes);
    }

    private boolean described(int location, int components, int type, int normalized,
                              int strideInBytes, int offsetInBytes) {
        Attributes held = attributes();
        int at = location * POINTER_FIELDS;
        if (at + POINTER_FIELDS > held.pointers.length) {
            held.pointers = Arrays.copyOf(held.pointers, at + POINTER_FIELDS);
        }
        int[] pointers = held.pointers;
        if (held.described.get(location)
                && pointers[at] == boundArrayBuffer
                && pointers[at + 1] == components
                && pointers[at + 2] == type
                && pointers[at + 3] == normalized
                && pointers[at + 4] == strideInBytes
                && pointers[at + 5] == offsetInBytes) {
            return true;
        }
        held.described.set(location);
        pointers[at] = boundArrayBuffer;
        pointers[at + 1] = components;
        pointers[at + 2] = type;
        pointers[at + 3] = normalized;
        pointers[at + 4] = strideInBytes;
        pointers[at + 5] = offsetInBytes;
        return false;
    }

    @Override
    public void integerAttributePointer(int location, int components, AttributeType type,
                                        int strideInBytes, int offsetInBytes) {
        requireAligned(type, strideInBytes, "stride");
        requireAligned(type, offsetInBytes, "offset");
        if (described(location, components, attributeType(type), 2, strideInBytes,
                offsetInBytes)) {
            return;
        }
        gl.vertexAttribIPointer(location, components, attributeType(type), strideInBytes,
                offsetInBytes);
    }

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
        int at = location * 4;
        if (at + 4 > constants.length) {
            constants = Arrays.copyOf(constants, at + 4);
        }
        if (constantsKnown.get(location) && constants[at] == x && constants[at + 1] == y
                && constants[at + 2] == z && constants[at + 3] == w) {
            return;
        }
        constantsKnown.set(location);
        constants[at] = x;
        constants[at + 1] = y;
        constants[at + 2] = z;
        constants[at + 3] = w;
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
        defaulted.clear(texture);

        for (int unit = 0; unit < boundTexture.length; unit++) {
            if (boundTexture[unit] == texture) {
                boundTexture[unit] = NO_TEXTURE;
            }
        }
    }

    @Override
    public void activeTexture(int unit) {
        selectUnit(unit);
    }

    private void selectUnit(int unit) {
        if (unit == activeUnit) {
            return;
        }
        activeUnit = unit;
        gl.activeTexture(WebGL2.TEXTURE0 + unit);
    }

    @Override
    public void bindTexture(int unit, int texture) {

        selectUnit(unit);
        boolean tracked = unit < boundTexture.length;
        if (tracked && boundTexture[unit] == texture
                && (texture == NO_TEXTURE || defaulted.get(texture))) {
            return;
        }
        gl.bindTexture(WebGL2.TEXTURE_2D, texture == NO_TEXTURE ? null : liveTexture(texture));
        if (tracked) {
            boundTexture[unit] = texture;
        }
        if (texture != NO_TEXTURE && !defaulted.get(texture)) {
            defaulted.set(texture);
            setTextureWrapS(TextureWrap.CLAMP_TO_EDGE);
            setTextureWrapT(TextureWrap.CLAMP_TO_EDGE);
            setTextureMinFilter(TextureFilter.NEAREST);
            setTextureMagFilter(TextureFilter.NEAREST);
        }
    }

    @Override
    public void textureImage(int level, int width, int height, byte[] rgba) {
        Uint8Array data = pixels(width, height, rgba);
        gl.texImage2D(WebGL2.TEXTURE_2D, level, WebGL2.RGBA, width, height, 0,
                WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, data);
    }

    @Override
    public void textureImageArgb(int level, int width, int height, int[] argb, int count) {
        gl.texImage2D(WebGL2.TEXTURE_2D, level, WebGL2.RGBA, width, height, 0,
                WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, rgba(argb, count));
    }

    @Override
    public void textureSubImageArgb(int level, int x, int y, int width, int height,
                                    int[] argb, int count) {
        gl.texSubImage2D(WebGL2.TEXTURE_2D, level, x, y, width, height,
                WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, rgba(argb, count));
    }

    private Uint8Array rgba(int[] argb, int count) {
        if (argb.length < count) {
            throw new IllegalArgumentException("need " + count + " pixels, got " + argb.length);
        }
        if (swizzled == null || swizzled.getLength() < count) {
            swizzled = new Uint32Array(count);
        }
        return JsArrays.rgba(argb, count, swizzled);
    }

    @Override
    public void textureSubImage(int level, int x, int y, int width, int height, byte[] rgba) {
        Uint8Array data = pixels(width, height, rgba);
        gl.texSubImage2D(WebGL2.TEXTURE_2D, level, x, y, width, height,
                WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, data);
    }

    @Override
    public void copyTextureSubImage(int x, int y, int framebufferX, int framebufferY,
                                    int width, int height) {
        gl.copyTexSubImage2D(WebGL2.TEXTURE_2D, 0, x, y, framebufferX, framebufferY, width, height);
    }

    @Override
    public byte[] readPixels(int x, int y, int width, int height) {
        Uint8Array data = Uint8Array.create(width * height * 4);
        gl.readPixels(x, y, width, height, WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, data);
        return JsArrays.bytesOf(Int8Array.create(data.getBuffer()));
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
    public int createFramebuffer() {
        return store(gl.createFramebuffer());
    }

    @Override
    public void bindFramebuffer(int framebuffer) {
        gl.bindFramebuffer(WebGL2.FRAMEBUFFER, framebuffer == NO_FRAMEBUFFER ? null
                : lookup(framebuffer, "framebuffer"));
        boundFramebuffer = framebuffer;
    }

    @Override
    public int boundFramebuffer() {
        return boundFramebuffer;
    }

    @Override
    public void deleteFramebuffer(int framebuffer) {
        if (framebuffer == NO_FRAMEBUFFER) {
            return;
        }
        if (boundFramebuffer == framebuffer) {
            bindFramebuffer(NO_FRAMEBUFFER);
        }
        gl.deleteFramebuffer(lookup(framebuffer, "framebuffer"));
        handles.remove(framebuffer);
    }

    @Override
    public void framebufferTexture(FramebufferAttachment attachment, int texture, int level) {
        gl.framebufferTexture2D(WebGL2.FRAMEBUFFER, attachmentConstant(attachment),
                WebGL2.TEXTURE_2D, texture == NO_TEXTURE ? null : lookup(texture, "texture"),
                level);
    }

    @Override
    public void framebufferRenderbuffer(FramebufferAttachment attachment, int renderbuffer) {
        gl.framebufferRenderbuffer(WebGL2.FRAMEBUFFER, attachmentConstant(attachment),
                WebGL2.RENDERBUFFER, renderbuffer == NO_RENDERBUFFER ? null
                        : lookup(renderbuffer, "renderbuffer"));
    }

    @Override
    public FramebufferStatus framebufferStatus() {
        return switch (gl.checkFramebufferStatus(WebGL2.FRAMEBUFFER)) {
            case WebGL2.FRAMEBUFFER_COMPLETE -> FramebufferStatus.COMPLETE;
            case WebGL2.FRAMEBUFFER_INCOMPLETE_ATTACHMENT ->
                    FramebufferStatus.INCOMPLETE_ATTACHMENT;
            case WebGL2.FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT ->
                    FramebufferStatus.INCOMPLETE_MISSING_ATTACHMENT;
            case WebGL2.FRAMEBUFFER_INCOMPLETE_DIMENSIONS ->
                    FramebufferStatus.INCOMPLETE_DIMENSIONS;
            case WebGL2.FRAMEBUFFER_UNSUPPORTED -> FramebufferStatus.UNSUPPORTED;
            default -> FramebufferStatus.UNKNOWN;
        };
    }

    @Override
    public int createRenderbuffer() {
        return store(gl.createRenderbuffer());
    }

    @Override
    public void bindRenderbuffer(int renderbuffer) {
        gl.bindRenderbuffer(WebGL2.RENDERBUFFER, renderbuffer == NO_RENDERBUFFER ? null
                : lookup(renderbuffer, "renderbuffer"));
    }

    @Override
    public void deleteRenderbuffer(int renderbuffer) {
        if (renderbuffer == NO_RENDERBUFFER) {
            return;
        }
        gl.deleteRenderbuffer(lookup(renderbuffer, "renderbuffer"));
        handles.remove(renderbuffer);
    }

    @Override
    public void renderbufferStorage(RenderbufferFormat format, int width, int height) {
        gl.renderbufferStorage(WebGL2.RENDERBUFFER, switch (format) {
            case RGBA8 -> WebGL2.RGBA8;
            case DEPTH16 -> WebGL2.DEPTH_COMPONENT16;
            case DEPTH24 -> WebGL2.DEPTH_COMPONENT24;
            case DEPTH24_STENCIL8 -> WebGL2.DEPTH24_STENCIL8;
            case STENCIL8 -> WebGL2.STENCIL_INDEX8;
        }, width, height);
    }

    @Override
    public void textureStorage(int level, int width, int height) {
        gl.texImage2D(WebGL2.TEXTURE_2D, level, WebGL2.RGBA, width, height, 0,
                WebGL2.RGBA, WebGL2.UNSIGNED_BYTE, null);
    }

    private static int attachmentConstant(FramebufferAttachment attachment) {
        return switch (attachment) {
            case COLOR -> WebGL2.COLOR_ATTACHMENT0;
            case DEPTH -> WebGL2.DEPTH_ATTACHMENT;
            case STENCIL -> WebGL2.STENCIL_ATTACHMENT;
            case DEPTH_STENCIL -> WebGL2.DEPTH_STENCIL_ATTACHMENT;
        };
    }

    private static Uint8Array pixels(int width, int height, byte[] rgba) {
        int needed = width * height * 4;
        if (rgba.length < needed) {
            throw new IllegalArgumentException(
                    "need " + needed + " bytes, got " + rgba.length);
        }

        return JsArrays.unsignedBytes(rgba, needed);
    }

    @Override
    public void generateMipmap() {
        gl.generateMipmap(WebGL2.TEXTURE_2D);
    }

    @Override
    public void setTextureBaseLevel(int level) {
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_BASE_LEVEL, level);
    }

    @Override
    public void setTextureMaxLevel(int level) {
        gl.texParameteri(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_MAX_LEVEL, level);
    }

    @Override
    public void setTextureMinLod(float lod) {
        gl.texParameterf(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_MIN_LOD, lod);
    }

    @Override
    public void setTextureMaxLod(float lod) {
        gl.texParameterf(WebGL2.TEXTURE_2D, WebGL2.TEXTURE_MAX_LOD, lod);
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
            case NEAREST_MIPMAP_NEAREST -> WebGL2.NEAREST_MIPMAP_NEAREST;
            case LINEAR_MIPMAP_NEAREST -> WebGL2.LINEAR_MIPMAP_NEAREST;
            case NEAREST_MIPMAP_LINEAR -> WebGL2.NEAREST_MIPMAP_LINEAR;
            case LINEAR_MIPMAP_LINEAR -> WebGL2.LINEAR_MIPMAP_LINEAR;
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

    @Override
    public void drawArraysInstanced(Primitive primitive, int first, int count, int instances) {
        gl.drawArraysInstanced(mode(primitive), first, count, instances);
    }

    @Override
    public void drawElementsInstanced(Primitive primitive, int count, IndexType type,
                                      int offsetInBytes, int instances) {
        gl.drawElementsInstanced(mode(primitive), count, indexType(type), offsetInBytes, instances);
    }

    @Override
    public void attributeDivisor(int location, int divisor) {
        gl.vertexAttribDivisor(location, divisor);
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
            case WebGL2.INVALID_FRAMEBUFFER_OPERATION ->
                    GraphicsError.INVALID_FRAMEBUFFER_OPERATION;
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
