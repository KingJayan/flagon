package dev.flagon.lwjgl.internal;

import dev.flagon.platform.graphics.GraphicsDevice;
import dev.flagon.platform.graphics.IndexType;

/**
 * draws through the application's own program
 */
public final class ShaderPipeline {

    private final GraphicsDevice device;
    private final StagingArrays staging;
    private final QuadIndices quadIndices;

    public ShaderPipeline(GraphicsDevice device, int attributes) {
        this.device = device;
        this.staging = new StagingArrays(device, attributes);
        this.quadIndices = new QuadIndices(device);
    }

    public void drawArrays(DrawMode mode, int first, int count, GenericArrays arrays) {
        int drawable = mode.usableVertices(count);
        if (drawable == 0 || !arrays.canDraw()) {
            return;
        }
        bind(arrays, first, drawable);
        if (mode.isIndexed()) {
            int quads = drawable / DrawMode.QUAD_VERTICES;
            quadIndices.bind(quads, first);
            device.drawElements(mode.primitive(), quads * DrawMode.QUAD_INDICES,
                    IndexType.UNSIGNED_INT, 0);
        } else {
            device.drawArrays(mode.primitive(), first, drawable);
        }
    }

    public void drawElements(DrawMode mode, int count, IndexType indexType, int offsetInBytes,
                             GenericArrays arrays) {
        if (count == 0 || !arrays.canDraw()) {
            return;
        }
        if (mode.isIndexed()) {
            throw new UnsupportedOperationException(
                    "glDrawElements cannot emulate GL_QUADS: expanding quads into triangle pairs "
                            + "means rewriting the index buffer, which belongs to the caller. draw "
                            + "the quads with glDrawArrays, or supply triangle indices");
        }
        bind(arrays, 0, StagingArrays.WHOLE);
        device.drawElements(mode.primitive(), count, indexType, offsetInBytes);
    }

    public void drawArraysInstanced(DrawMode mode, int first, int count, int instances,
                                    GenericArrays arrays) {
        if (mode.isIndexed()) {
            throw new UnsupportedOperationException(
                    "glDrawArraysInstanced cannot emulate GL_QUADS; the core profile has no quads");
        }
        if (count == 0 || instances == 0 || !arrays.canDraw()) {
            return;
        }
        bind(arrays, first, count);
        device.drawArraysInstanced(mode.primitive(), first, count, instances);
    }

    public void drawElementsInstanced(DrawMode mode, int count, IndexType indexType,
                                      int offsetInBytes, int instances, GenericArrays arrays) {
        if (mode.isIndexed()) {
            throw new UnsupportedOperationException(
                    "glDrawElementsInstanced cannot emulate GL_QUADS; the core profile has no"
                            + " quads");
        }
        if (count == 0 || instances == 0 || !arrays.canDraw()) {
            return;
        }
        bind(arrays, 0, StagingArrays.WHOLE);
        device.drawElementsInstanced(mode.primitive(), count, indexType, offsetInBytes, instances);
    }

    private void bind(GenericArrays arrays, int first, int count) {
        for (int slot = 0; slot < arrays.count(); slot++) {
            ClientArray array = arrays.slot(slot);
            if (!array.isEnabled() || !array.isBound()) {
                device.disableAttribute(slot);
                continue;
            }
            device.enableAttribute(slot);
            staging.bind(slot, array, first, count);
            device.attributeDivisor(slot, array.divisor());
        }
    }

    public void dispose() {
        staging.dispose();
        quadIndices.dispose();
    }
}
