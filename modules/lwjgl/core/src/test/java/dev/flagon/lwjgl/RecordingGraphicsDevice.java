package dev.flagon.lwjgl;

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
import dev.flagon.platform.graphics.ShaderStage;
import dev.flagon.platform.graphics.StencilOp;
import dev.flagon.platform.graphics.Winding;
import dev.flagon.platform.graphics.TextureFilter;
import dev.flagon.platform.graphics.TextureWrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * records every call instead of drawing; nothing above platform-api needs a browser.
 */
final class RecordingGraphicsDevice implements GraphicsDevice {

    final List<String> calls = new ArrayList<>();
    float[] lastBufferData = new float[0];
    int[] lastElementData = new int[0];
    byte[] lastByteData = new byte[0];
    byte[] lastRgba = new byte[0];
    float[] lastUniformFloats = new float[0];
    int[] lastUniformInts = new int[0];
    float[] lastModelView = new float[16];
    float[] lastProjection = new float[16];

    private final Map<Integer, String> uniformNames = new HashMap<>();
    private final Map<String, float[]> uniformFloatValues = new HashMap<>();
    private final Map<String, int[]> uniformIntValues = new HashMap<>();

    private final Map<Integer, String> shaderSources = new HashMap<>();
    private int nextName = 1;
    boolean compilesShaders = true;
    boolean linksPrograms = true;
    private int modelViewLocation = -1;
    private int projectionLocation = -1;

    @Override
    public void viewport(int x, int y, int width, int height) {
        calls.add("viewport " + x + " " + y + " " + width + " " + height);
    }

    @Override
    public void clearColor(float red, float green, float blue, float alpha) {
        calls.add("clearColor " + red + " " + green + " " + blue + " " + alpha);
    }

    @Override
    public void clearDepth(double depth) {
        calls.add("clearDepth " + depth);
    }

    @Override
    public void clear(boolean color, boolean depth, boolean stencil) {
        calls.add("clear " + color + " " + depth + " " + stencil);
    }

    @Override
    public void setDepthTest(boolean enabled) {
        calls.add("depthTest " + enabled);
    }

    @Override
    public void setDepthWrite(boolean enabled) {
        calls.add("depthWrite " + enabled);
    }

    @Override
    public void setDepthFunc(CompareFunc func) {
        calls.add("depthFunc " + func);
    }

    @Override
    public void setBlend(boolean enabled) {
        calls.add("blend " + enabled);
    }

    @Override
    public void setBlendFunc(BlendFactor source, BlendFactor destination) {
        calls.add("blendFunc " + source + " " + destination);
    }

    @Override
    public void setBlendFuncSeparate(BlendFactor sourceColor, BlendFactor destinationColor,
                                     BlendFactor sourceAlpha, BlendFactor destinationAlpha) {
        calls.add("blendFuncSeparate " + sourceColor + " " + destinationColor + " "
                + sourceAlpha + " " + destinationAlpha);
    }

    @Override
    public void setBlendEquation(BlendEquation equation) {
        calls.add("blendEquation " + equation);
    }

    @Override
    public void setBlendEquationSeparate(BlendEquation color, BlendEquation alpha) {
        calls.add("blendEquationSeparate " + color + " " + alpha);
    }

    @Override
    public void setBlendColor(float red, float green, float blue, float alpha) {
        calls.add("blendColor " + red + " " + green + " " + blue + " " + alpha);
    }

    @Override
    public void setCullFace(boolean enabled) {
        calls.add("cullFace " + enabled);
    }

    @Override
    public void setCulledFaces(CullFace faces) {
        calls.add("culledFaces " + faces);
    }

    @Override
    public void setFrontFace(Winding winding) {
        calls.add("frontFace " + winding);
    }

    @Override
    public void setScissorTest(boolean enabled) {
        calls.add("scissorTest " + enabled);
    }

    @Override
    public void setScissor(int x, int y, int width, int height) {
        calls.add("scissor " + x + "," + y + " " + width + "x" + height);
    }

    @Override
    public void setColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        calls.add("colorMask " + red + " " + green + " " + blue + " " + alpha);
    }

    @Override
    public int createBuffer() {
        return nextName++;
    }

    @Override
    public void deleteBuffer(int buffer) {
        calls.add("deleteBuffer " + buffer);
    }

    @Override
    public void bindBuffer(BufferTarget target, int buffer) {
        calls.add("bind" + target + " " + buffer);
    }

    @Override
    public void bufferData(BufferTarget target, int sizeInBytes, BufferUsage usage) {
        calls.add("reserve" + target + " " + sizeInBytes + " " + usage);
    }

    @Override
    public void bufferData(BufferTarget target, byte[] data, int length, BufferUsage usage) {
        lastByteData = new byte[length];
        System.arraycopy(data, 0, lastByteData, 0, length);
        calls.add("byteData" + target + " " + length + " " + usage);
    }

    @Override
    public void bufferData(BufferTarget target, float[] data, int length, BufferUsage usage) {
        lastBufferData = new float[length];
        System.arraycopy(data, 0, lastBufferData, 0, length);
        calls.add("bufferData " + length + " " + usage);
    }

    @Override
    public void bufferData(BufferTarget target, int[] data, int length, BufferUsage usage) {
        lastElementData = new int[length];
        System.arraycopy(data, 0, lastElementData, 0, length);
        calls.add("elementData " + length + " " + usage);
    }

    @Override
    public void bufferSubData(BufferTarget target, int offsetInBytes, byte[] data, int length) {
        lastByteData = new byte[length];
        System.arraycopy(data, 0, lastByteData, 0, length);
        calls.add("byteSubData" + target + " " + offsetInBytes + " " + length);
    }

    @Override
    public void bufferSubData(BufferTarget target, int offsetInBytes, float[] data, int length) {
        lastBufferData = new float[length];
        System.arraycopy(data, 0, lastBufferData, 0, length);
        calls.add("bufferSubData " + offsetInBytes + " " + length);
    }

    @Override
    public int createProgram(String vertexSource, String fragmentSource) {
        return nextName++;
    }

    @Override
    public int createShader(ShaderStage stage) {
        int shader = nextName++;
        calls.add("createShader " + stage + " " + shader);
        return shader;
    }

    @Override
    public void shaderSource(int shader, String source) {
        shaderSources.put(shader, source);
        calls.add("shaderSource " + shader);
    }

    @Override
    public void compileShader(int shader) {
        calls.add("compileShader " + shader);
    }

    @Override
    public boolean isShaderCompiled(int shader) {
        return compilesShaders;
    }

    @Override
    public String shaderInfoLog(int shader) {
        return compilesShaders ? "" : "compile failed";
    }

    @Override
    public void deleteShader(int shader) {
        shaderSources.remove(shader);
        calls.add("deleteShader " + shader);
    }

    @Override
    public boolean isShader(int shader) {
        return shaderSources.containsKey(shader);
    }

    @Override
    public int createProgram() {
        int program = nextName++;
        calls.add("createProgram " + program);
        return program;
    }

    @Override
    public void attachShader(int program, int shader) {
        calls.add("attachShader " + program + " " + shader);
    }

    @Override
    public void detachShader(int program, int shader) {
        calls.add("detachShader " + program + " " + shader);
    }

    @Override
    public void bindAttributeLocation(int program, int location, String name) {
        calls.add("bindAttributeLocation " + program + " " + location + " " + name);
    }

    @Override
    public void linkProgram(int program) {
        calls.add("linkProgram " + program);
    }

    @Override
    public boolean isProgramLinked(int program) {
        return linksPrograms;
    }

    @Override
    public void validateProgram(int program) {
        calls.add("validateProgram " + program);
    }

    @Override
    public boolean isProgramValid(int program) {
        return linksPrograms;
    }

    @Override
    public String programInfoLog(int program) {
        return linksPrograms ? "" : "link failed";
    }

    @Override
    public boolean isProgram(int program) {
        return program > 0;
    }

    @Override
    public void deleteProgram(int program) {
        calls.add("deleteProgram " + program);
    }

    String sourceOf(int shader) {
        return shaderSources.get(shader);
    }

    @Override
    public void useProgram(int program) {
        calls.add("useProgram " + program);
    }

    @Override
    public int uniformLocation(int program, String name) {
        int location = nextName++;
        uniformNames.put(location, name);
        if (name.equals("u_modelView")) {
            modelViewLocation = location;
        } else if (name.equals("u_projection")) {
            projectionLocation = location;
        }
        return location;
    }

    @Override
    public void uniformMatrix4(int location, float[] matrix) {
        if (location == modelViewLocation) {
            lastModelView = matrix.clone();
        } else if (location == projectionLocation) {
            lastProjection = matrix.clone();
        }
        calls.add("uniformMatrix4 " + location);
    }

    @Override
    public void uniform1i(int location, int value) {
        remember(location, new int[] {value});
        calls.add("uniform1i " + location + " " + value);
    }

    private void remember(int location, float[] values) {
        String name = uniformNames.get(location);
        if (name != null) {
            uniformFloatValues.put(name, values);
        }
    }

    private void remember(int location, int[] values) {
        String name = uniformNames.get(location);
        if (name != null) {
            uniformIntValues.put(name, values);
        }
    }

    /** @return what the named uniform was last given, or null when the shader never got it */
    float[] uniformFloats(String name) {
        return uniformFloatValues.get(name);
    }

    int[] uniformInts(String name) {
        return uniformIntValues.get(name);
    }

    @Override
    public void uniform1f(int location, float value) {
        remember(location, new float[] {value});
        calls.add("uniform1f " + location + " " + value);
    }

    @Override
    public void uniform2f(int location, float x, float y) {
        calls.add("uniform2f " + location + " " + x + " " + y);
    }

    @Override
    public void uniform3f(int location, float x, float y, float z) {
        calls.add("uniform3f " + location + " " + x + " " + y + " " + z);
    }

    @Override
    public void uniform4f(int location, float x, float y, float z, float w) {
        remember(location, new float[] {x, y, z, w});
        calls.add("uniform4f " + location);
    }

    @Override
    public void uniform2i(int location, int x, int y) {
        calls.add("uniform2i " + location + " " + x + " " + y);
    }

    @Override
    public void uniform3i(int location, int x, int y, int z) {
        calls.add("uniform3i " + location + " " + x + " " + y + " " + z);
    }

    @Override
    public void uniform4i(int location, int x, int y, int z, int w) {
        calls.add("uniform4i " + location + " " + x + " " + y + " " + z + " " + w);
    }

    @Override
    public void uniformFloats(int location, int components, float[] values, int count) {
        lastUniformFloats = new float[components * count];
        System.arraycopy(values, 0, lastUniformFloats, 0, lastUniformFloats.length);
        remember(location, lastUniformFloats);
        calls.add("uniformFloats " + location + " " + components + " " + count);
    }

    @Override
    public void uniformInts(int location, int components, int[] values, int count) {
        lastUniformInts = new int[components * count];
        System.arraycopy(values, 0, lastUniformInts, 0, lastUniformInts.length);
        remember(location, lastUniformInts);
        calls.add("uniformInts " + location + " " + components + " " + count);
    }

    @Override
    public void uniformMatrices(int location, int size, boolean transpose, float[] values,
                                int count) {
        lastUniformFloats = new float[size * size * count];
        System.arraycopy(values, 0, lastUniformFloats, 0, lastUniformFloats.length);
        remember(location, lastUniformFloats);
        calls.add("uniformMatrices " + location + " " + size + " " + transpose + " " + count);
    }

    @Override
    public int attributeLocation(int program, String name) {
        return nextName++;
    }

    @Override
    public int maxAttributes() {
        return 16;
    }

    @Override
    public int createVertexArray() {
        int array = nextName++;
        calls.add("createVertexArray " + array);
        return array;
    }

    @Override
    public void bindVertexArray(int array) {
        calls.add("bindVertexArray " + array);
    }

    @Override
    public void deleteVertexArray(int array) {
        calls.add("deleteVertexArray " + array);
    }

    @Override
    public void enableAttribute(int location) {
        calls.add("enableAttribute " + location);
    }

    @Override
    public void disableAttribute(int location) {
        calls.add("disableAttribute " + location);
    }

    @Override
    public void attributePointer(int location, int components, AttributeType type,
                                 boolean normalized, int strideInBytes, int offsetInBytes) {
        calls.add("attributePointer " + location + " " + components + " " + type + " "
                + normalized + " " + strideInBytes + " " + offsetInBytes);
    }

    private int nextFramebuffer = 1;
    private int nextRenderbuffer = 1;
    private int boundFramebuffer = NO_FRAMEBUFFER;

    /** the pixels the last upload handed over, so a test can read what reached the device. */
    public byte[] lastTextureBytes = new byte[0];
    FramebufferStatus status = FramebufferStatus.COMPLETE;

    @Override
    public int createFramebuffer() {
        int framebuffer = nextFramebuffer++;
        calls.add("createFramebuffer " + framebuffer);
        return framebuffer;
    }

    @Override
    public void bindFramebuffer(int framebuffer) {
        boundFramebuffer = framebuffer;
        calls.add("bindFramebuffer " + framebuffer);
    }

    @Override
    public int boundFramebuffer() {
        return boundFramebuffer;
    }

    @Override
    public void deleteFramebuffer(int framebuffer) {
        if (boundFramebuffer == framebuffer) {
            boundFramebuffer = NO_FRAMEBUFFER;
        }
        calls.add("deleteFramebuffer " + framebuffer);
    }

    @Override
    public void framebufferTexture(FramebufferAttachment attachment, int texture, int level) {
        calls.add("framebufferTexture " + attachment + " " + texture + " " + level);
    }

    @Override
    public void framebufferRenderbuffer(FramebufferAttachment attachment, int renderbuffer) {
        calls.add("framebufferRenderbuffer " + attachment + " " + renderbuffer);
    }

    @Override
    public FramebufferStatus framebufferStatus() {
        return status;
    }

    @Override
    public int createRenderbuffer() {
        int renderbuffer = nextRenderbuffer++;
        calls.add("createRenderbuffer " + renderbuffer);
        return renderbuffer;
    }

    @Override
    public void bindRenderbuffer(int renderbuffer) {
        calls.add("bindRenderbuffer " + renderbuffer);
    }

    @Override
    public void deleteRenderbuffer(int renderbuffer) {
        calls.add("deleteRenderbuffer " + renderbuffer);
    }

    @Override
    public void renderbufferStorage(RenderbufferFormat format, int width, int height) {
        calls.add("renderbufferStorage " + format + " " + width + "x" + height);
    }

    @Override
    public void textureStorage(int level, int width, int height) {
        calls.add("textureStorage " + level + " " + width + "x" + height);
    }

    @Override
    public void integerAttributePointer(int location, int components, AttributeType type,
                                        int strideInBytes, int offsetInBytes) {
        calls.add("integerAttributePointer " + location + " " + components + " " + type + " "
                + strideInBytes + " " + offsetInBytes);
    }

    @Override
    public void constantAttribute(int location, float x, float y, float z, float w) {
        calls.add("constantAttribute " + location + " " + x + " " + y + " " + z + " " + w);
    }

    @Override
    public int createTexture() {
        return nextName++;
    }

    @Override
    public void deleteTexture(int texture) {
        calls.add("deleteTexture " + texture);
    }

    @Override
    public void bindTexture(int unit, int texture) {
        calls.add("bindTexture " + unit + " " + texture);
    }

    @Override
    public void textureImage(int level, int width, int height, byte[] rgba) {
        lastTextureBytes = kept(rgba, width * height * 4);
        calls.add("textureImage " + level + " " + width + "x" + height);
        lastRgba = lastTextureBytes;
    }

    // a device reads only the bytes the rectangle names
    private static byte[] kept(byte[] rgba, int needed) {
        return rgba.length == needed ? rgba : Arrays.copyOf(rgba, needed);
    }

    @Override
    public void textureSubImage(int level, int x, int y, int width, int height, byte[] rgba) {
        lastTextureBytes = kept(rgba, width * height * 4);
        calls.add("textureSubImage " + level + " " + x + "," + y + " " + width + "x" + height);
        lastRgba = lastTextureBytes;
    }

    @Override
    public void setTextureMinFilter(TextureFilter filter) {
        calls.add("textureMinFilter " + filter);
    }

    @Override
    public void setTextureMagFilter(TextureFilter filter) {
        calls.add("textureMagFilter " + filter);
    }

    @Override
    public void setTextureWrapS(TextureWrap wrap) {
        calls.add("textureWrapS " + wrap);
    }

    @Override
    public void setTextureWrapT(TextureWrap wrap) {
        calls.add("textureWrapT " + wrap);
    }

    @Override
    public void drawArrays(Primitive primitive, int first, int count) {
        calls.add("drawArrays " + primitive + " " + first + " " + count);
    }

    @Override
    public void drawElements(Primitive primitive, int count, IndexType type, int offsetInBytes) {
        calls.add("drawElements " + primitive + " " + count + " " + type + " " + offsetInBytes);
    }

    @Override
    public void drawArraysInstanced(Primitive primitive, int first, int count, int instances) {
        calls.add("drawArraysInstanced " + primitive + " " + first + " " + count + " " + instances);
    }

    @Override
    public void drawElementsInstanced(Primitive primitive, int count, IndexType type,
                                      int offsetInBytes, int instances) {
        calls.add("drawElementsInstanced " + primitive + " " + count + " " + type + " "
                + offsetInBytes + " " + instances);
    }

    @Override
    public void attributeDivisor(int location, int divisor) {
        calls.add("attributeDivisor " + location + " " + divisor);
    }

    @Override
    public int width() {
        return 854;
    }

    @Override
    public int height() {
        return 480;
    }

    GraphicsError nextError = GraphicsError.NONE;

    @Override
    public GraphicsError takeError() {
        GraphicsError error = nextError;
        nextError = GraphicsError.NONE;
        return error;
    }

    String extensions = "";

    @Override
    public String information(GraphicsInfo information) {
        if (information == GraphicsInfo.EXTENSIONS) {
            return extensions;
        }
        return "recorded " + information;
    }

    @Override
    public int limit(GraphicsLimit limit) {
        return switch (limit) {
            case MAX_TEXTURE_SIZE -> 4096;
            case MAX_TEXTURE_UNITS -> 16;
            case MAX_VERTEX_ATTRIBUTES -> 8;
            case MAX_VERTEX_UNIFORM_COMPONENTS -> 1024;
            case MAX_FRAGMENT_UNIFORM_COMPONENTS -> 896;
            case MAX_VARYING_COMPONENTS -> 60;
            case MAX_VERTEX_TEXTURE_UNITS -> 16;
            case MAX_COMBINED_TEXTURE_UNITS -> 32;
            case MAX_ARRAY_TEXTURE_LAYERS -> 256;
        };
    }

    List<String> calls(String name) {
        return calls.stream().filter(call -> call.startsWith(name + " ")).toList();
    }

    @Override
    public void setStencilTest(boolean enabled) {
        calls.add("stencilTest " + enabled);
    }

    @Override
    public void setStencilFunc(CompareFunc func, int reference, int mask) {
        calls.add("stencilFunc " + func + " " + reference + " " + mask);
    }

    @Override
    public void setStencilOp(StencilOp stencilFail, StencilOp depthFail, StencilOp pass) {
        calls.add("stencilOp " + stencilFail + " " + depthFail + " " + pass);
    }

    @Override
    public void setStencilMask(int mask) {
        calls.add("stencilMask " + mask);
    }

    @Override
    public void clearStencil(int value) {
        calls.add("clearStencil " + value);
    }

    @Override
    public void setPolygonOffsetFill(boolean enabled) {
        calls.add("polygonOffsetFill " + enabled);
    }

    @Override
    public void setPolygonOffset(float factor, float units) {
        calls.add("polygonOffset " + factor + " " + units);
    }

    @Override
    public void setLineWidth(float width) {
        calls.add("lineWidth " + width);
    }

    @Override
    public void copyTextureSubImage(int x, int y, int framebufferX, int framebufferY,
                                    int width, int height) {
        calls.add("copyTextureSubImage " + x + " " + y + " " + framebufferX + " " + framebufferY
                + " " + width + " " + height);
    }

    /** a framebuffer of one repeated color, so a read has something predictable in it. */
    public byte[] framebuffer = new byte[] {(byte) 0x10, (byte) 0x20, (byte) 0x30, (byte) 0x40};

    @Override
    public byte[] readPixels(int x, int y, int width, int height) {
        calls.add("readPixels " + x + " " + y + " " + width + " " + height);
        byte[] read = new byte[width * height * 4];
        for (int index = 0; index < read.length; index++) {
            read[index] = framebuffer[index % framebuffer.length];
        }
        return read;
    }

    @Override
    public void finish() {
        calls.add("finish");
    }

    @Override
    public void flush() {
        calls.add("flush");
    }

    @Override
    public void generateMipmap() {
        calls.add("generateMipmap");
    }

    @Override
    public void setTextureBaseLevel(int level) {
        calls.add("textureBaseLevel " + level);
    }

    @Override
    public void setTextureMaxLevel(int level) {
        calls.add("textureMaxLevel " + level);
    }

    @Override
    public void setTextureMinLod(float lod) {
        calls.add("textureMinLod " + lod);
    }

    @Override
    public void setTextureMaxLod(float lod) {
        calls.add("textureMaxLod " + lod);
    }

    @Override
    public void activeTexture(int unit) {
        calls.add("activeTexture " + unit);
    }
}
