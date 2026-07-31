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
        bind(arrays);
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
        bind(arrays);
        device.drawElements(mode.primitive(), count, indexType, offsetInBytes);
    }

    private void bind(GenericArrays arrays) {
        for (int slot = 0; slot < arrays.count(); slot++) {
            ClientArray array = arrays.slot(slot);
            if (!array.isEnabled() || !array.isBound()) {
                device.disableAttribute(slot);
                continue;
            }
            device.enableAttribute(slot);
            staging.bind(slot, array);
        }
    }

    public void dispose() {
        staging.dispose();
        quadIndices.dispose();
    }
}
