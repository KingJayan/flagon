package dev.flagon.platform.browser.graphics;

import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.browser.JsFake;
import dev.flagon.platform.graphics.AttributeType;
import dev.flagon.platform.graphics.BlendFactor;
import dev.flagon.platform.graphics.CompareFunc;
import dev.flagon.platform.graphics.CullFace;
import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.GraphicsError;
import dev.flagon.platform.graphics.GraphicsInfo;
import dev.flagon.platform.graphics.GraphicsLimit;
import dev.flagon.platform.graphics.Primitive;
import dev.flagon.platform.graphics.ShaderException;
import dev.flagon.platform.graphics.ShaderStage;
import dev.flagon.platform.graphics.Winding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebGLGraphicsDeviceTest {

    private JsFake context;
    private WebGLGraphicsDevice device;

    @BeforeEach
    void setUp() {
        context = new JsFake();
        device = new WebGLGraphicsDevice(context.of(WebGL2.class));
    }

    /** webgl2 core draws nothing without one, and no application knows to bind it */
    @Test
    void bindsAVertexArrayBeforeAnythingElse() {
        assertEquals(1, context.count("createVertexArray"));
        assertEquals(1, context.count("bindVertexArray"));
    }

    @Test
    void scissorsAndCullsThroughTheCallsWebglNamesThemBy() {
        device.setScissorTest(true);
        device.setScissor(1, 2, 3, 4);
        device.setCulledFaces(CullFace.FRONT_AND_BACK);
        device.setFrontFace(Winding.CLOCKWISE);

        assertTrue(context.called("enable(3089)"));
        assertEquals("scissor(1, 2, 3, 4)", context.lastCall("scissor"));
        assertEquals("cullFace(1032)", context.lastCall("cullFace"));
        assertEquals("frontFace(2304)", context.lastCall("frontFace"));
    }

    @Test
    void clearsOnlyTheBuffersItWasAskedFor() {
        device.clear(true, false, false);
        assertEquals("clear(16384)", context.lastCall("clear"));

        device.clear(true, true, true);
        assertEquals("clear(17664)", context.lastCall("clear"));

        device.clear(false, false, false);
        assertEquals(2, context.count("clear"), "a clear of nothing is not a call");
    }

    @Test
    void turnsCapabilitiesOnAndOffRatherThanSettingThem() {
        device.setDepthTest(true);
        device.setBlend(false);
        device.setCullFace(true);

        assertTrue(context.called("enable(2929)"));
        assertTrue(context.called("disable(3042)"));
        assertTrue(context.called("enable(2884)"));
    }

    @Test
    void mapsEveryCompareFunc() {
        for (CompareFunc func : CompareFunc.values()) {
            device.setDepthFunc(func);
        }

        assertEquals(CompareFunc.values().length, context.count("depthFunc"));
        assertEquals("depthFunc(519)", context.lastCall("depthFunc"));
    }

    @Test
    void mapsEveryBlendFactor() {
        for (BlendFactor factor : BlendFactor.values()) {
            device.setBlendFunc(factor, BlendFactor.ONE);
        }

        assertEquals(BlendFactor.values().length, context.count("blendFunc"));
        assertTrue(context.called("blendFunc(770, 1)"), context.calls().toString());
    }

    @Test
    void mapsEveryPrimitive() {
        for (Primitive primitive : Primitive.values()) {
            device.drawArrays(primitive, 0, 3);
        }

        assertEquals(Primitive.values().length, context.count("drawArrays"));
        assertTrue(context.called("drawArrays(4, 0, 3)"), context.calls().toString());
    }

    // names

    @Test
    void handsOutADistinctNameForEveryObject() {
        int first = device.createBuffer();
        int second = device.createBuffer();
        int texture = device.createTexture();

        assertNotEquals(first, second);
        assertNotEquals(second, texture);
        assertNotEquals(GraphicsDevice.NO_BUFFER, first);
    }

    @Test
    void refusesToUseANameThatWasDeleted() {
        int buffer = device.createBuffer();
        device.deleteBuffer(buffer);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> device.deleteBuffer(buffer));
        assertTrue(thrown.getMessage().contains("buffer"), thrown.getMessage());
    }

    @Test
    void bindsNothingForNameZeroInsteadOfLookingItUp() {
        // bound to something first: unbinding what is already unbound is a call the device skips
        device.bindBuffer(dev.flagon.platform.graphics.BufferTarget.ARRAY, device.createBuffer());
        device.bindBuffer(dev.flagon.platform.graphics.BufferTarget.ARRAY,
                GraphicsDevice.NO_BUFFER);

        assertEquals("bindBuffer(34962, null)", context.lastCall("bindBuffer"));
    }

    @Test
    void bindsTheSameBufferOnlyOnce() {
        int buffer = device.createBuffer();
        device.bindBuffer(dev.flagon.platform.graphics.BufferTarget.ARRAY, buffer);
        device.bindBuffer(dev.flagon.platform.graphics.BufferTarget.ARRAY, buffer);

        assertEquals(1, context.count("bindBuffer"));
    }

    @Test
    void setsTheSameUniformOnlyOnce() {
        int program = device.createProgram("vertex", "fragment");
        int location = device.uniformLocation(program, "u_value");

        device.uniform1f(location, 1.0f);
        device.uniform1f(location, 1.0f);
        device.uniform1f(location, 2.0f);

        assertEquals(2, context.count("uniform1f"));
    }

    @Test
    void enablesACapabilityOnlyOnce() {
        device.setDepthTest(true);
        device.setDepthTest(true);
        device.setDepthTest(false);

        assertEquals(1, context.count("enable"));
        assertEquals(1, context.count("disable"));
    }

    @Test
    void unbindsAProgramWithoutLookingUpZero() {
        device.useProgram(GraphicsDevice.NO_PROGRAM);

        assertEquals("useProgram(null)", context.lastCall("useProgram"));
    }

    @Test
    void reportsADeletedShaderAsNoLongerAShader() {
        int shader = device.createShader(ShaderStage.VERTEX);
        assertTrue(device.isShader(shader));

        device.deleteShader(shader);

        assertFalse(device.isShader(shader));
    }

    // shaders

    @Test
    void namesTheStageAShaderFailedIn() {
        context.answer("getShaderParameter", false).answer("getShaderInfoLog", "syntax error");

        ShaderException thrown = assertThrows(ShaderException.class,
                () -> device.createProgram("void main() {}", "void main() {}"));

        assertTrue(thrown.getMessage().startsWith("vertex shader failed"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("syntax error"), thrown.getMessage());
        assertEquals(1, context.count("deleteShader"), "the failed shader was left behind");
    }

    @Test
    void givesUpTheProgramWhenItWillNotLink() {
        context.answer("getProgramParameter", false).answer("getProgramInfoLog", "no main");

        ShaderException thrown = assertThrows(ShaderException.class,
                () -> device.createProgram("void main() {}", "void main() {}"));

        assertTrue(thrown.getMessage().contains("no main"), thrown.getMessage());
        assertEquals(1, context.count("deleteProgram"));
    }

    @Test
    void dropsTheShadersOnceTheProgramHasLinked() {
        device.createProgram("void main() {}", "void main() {}");

        assertEquals(2, context.count("deleteShader"));
        assertEquals(0, context.count("deleteProgram"));
    }

    @Test
    void answersAnEmptyLogRatherThanNullWhereThereIsNone() {
        context.answer("getShaderInfoLog", null);
        int shader = device.createShader(ShaderStage.FRAGMENT);

        assertEquals("", device.shaderInfoLog(shader));
    }

    // uniforms

    @Test
    void resolvesAUniformOnceAndRemembersIt() {
        int program = device.createProgram();

        int first = device.uniformLocation(program, "u_scale");
        int again = device.uniformLocation(program, "u_scale");

        assertEquals(first, again);
        assertEquals(1, context.count("getUniformLocation"));
    }

    @Test
    void reportsAUniformTheShaderDoesNotDeclareAsMinusOne() {
        context.answer("getUniformLocation", null);
        int program = device.createProgram();

        assertEquals(-1, device.uniformLocation(program, "u_notThere"));
    }

    /** writing to a uniform the shader dropped is a no-op in gl, and has to stay one here */
    @Test
    void writingToAnAbsentUniformDoesNothing() {
        device.uniform1f(-1, 0.5f);

        assertEquals(0, context.count("uniform1f"));
    }

    @Test
    void refusesALocationThatBelongsToNoProgram() {
        assertThrows(IllegalArgumentException.class, () -> device.uniform1i(9999, 1));
    }

    @Test
    void lettingAProgramGoLetsItsUniformLocationsGoToo() {
        int program = device.createProgram();
        int location = device.uniformLocation(program, "u_scale");

        device.deleteProgram(program);

        assertThrows(IllegalArgumentException.class, () -> device.uniform1f(location, 1.0f));
    }

    @Test
    void resolvesTheSameNameAgainForANewProgram() {
        int first = device.createProgram();
        device.uniformLocation(first, "u_scale");
        device.deleteProgram(first);

        int second = device.createProgram();
        int location = device.uniformLocation(second, "u_scale");

        assertEquals(2, context.count("getUniformLocation"));
        device.uniform1f(location, 1.0f);
        assertEquals(1, context.count("uniform1f"));
    }

    // attributes

    @Test
    void namesAnUnalignedAttributeRatherThanDrawingNothing() {
        UnsupportedCapabilityException thrown = assertThrows(UnsupportedCapabilityException.class,
                () -> device.attributePointer(0, 3, AttributeType.FLOAT, false, 15, 0));

        assertTrue(thrown.getMessage().contains("stride"), thrown.getMessage());

        assertThrows(UnsupportedCapabilityException.class,
                () -> device.attributePointer(0, 3, AttributeType.FLOAT, false, 16, 2));
    }

    @Test
    void acceptsAnAlignedAttribute() {
        device.attributePointer(1, 3, AttributeType.FLOAT, false, 16, 4);

        assertEquals("vertexAttribPointer(1, 3, 5126, false, 16, 4)",
                context.lastCall("vertexAttribPointer"));
    }

    /** a byte attribute is aligned at every offset, which the check must not get in the way of */
    @Test
    void leavesAByteAttributeAlone() {
        device.attributePointer(2, 4, AttributeType.UNSIGNED_BYTE, true, 15, 3);

        assertEquals(1, context.count("vertexAttribPointer"));
    }

    // errors

    @Test
    void reportsNoErrorWhereGlHasNone() {
        context.answer("getError", 0);

        assertEquals(GraphicsError.NONE, device.takeError());
    }

    @Test
    void namesTheErrorGlRaised() {
        context.answer("getError", 0x0501);

        assertEquals(GraphicsError.INVALID_VALUE, device.takeError());
    }

    /** a code webgl does not document is a lost context; every call after it fails anyway. */
    @Test
    void treatsAnUnknownCodeAsALostContext() {
        context.answer("getError", 0x9999);

        assertEquals(GraphicsError.CONTEXT_LOST, device.takeError());
    }

    @Test
    void passesOnWhatTheHostSaysAboutItself() {
        context.answer("getParameterString", "WebGL 2.0");

        assertEquals("WebGL 2.0", device.information(GraphicsInfo.VERSION));
    }

    @Test
    void aHostThatSaysNothingDescribesItselfAsNothing() {
        context.answer("getParameterString", null);
        context.answer("getSupportedExtensions", null);

        assertEquals("", device.information(GraphicsInfo.RENDERER));
        assertEquals("", device.information(GraphicsInfo.EXTENSIONS));
    }

    @Test
    void readsEachLimitFromTheParameterThatCarriesIt() {
        context.answer("getParameter", 8192);

        assertEquals(8192, device.limit(GraphicsLimit.MAX_TEXTURE_SIZE));
        assertEquals("getParameter(3379)", context.lastCall("getParameter"));

        device.limit(GraphicsLimit.MAX_TEXTURE_UNITS);
        assertEquals("getParameter(34930)", context.lastCall("getParameter"));

        device.limit(GraphicsLimit.MAX_VERTEX_ATTRIBUTES);
        assertEquals("getParameter(34921)", context.lastCall("getParameter"));
    }

    @Test
    void bindsATextureToTheUnitItWasGiven() {
        int texture = device.createTexture();

        device.bindTexture(3, texture);

        assertTrue(context.called("activeTexture(33987)"), context.calls().toString());
    }

    @Test
    void makesANewTextureWhenADeletedNameIsBound() {
        int texture = device.createTexture();
        device.deleteTexture(texture);

        device.bindTexture(0, texture);

        assertEquals(2, context.count("createTexture"));
    }

    @Test
    void makesATextureForANameItNeverHandedOut() {
        device.bindTexture(0, 7);

        assertEquals(1, context.count("createTexture"));
        assertNotEquals(7, device.createTexture());
    }
}
