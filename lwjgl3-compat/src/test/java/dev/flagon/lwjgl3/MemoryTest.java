package dev.flagon.lwjgl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.junit.jupiter.api.Test;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

class MemoryTest {

    @Test
    void handsOutAnAddressForAnAllocation() {
        ByteBuffer buffer = MemoryUtil.memAlloc(8);

        assertEquals(buffer.get(0), MemoryUtil.memByteBuffer(MemoryUtil.memAddress(buffer), 8).get(0));
        assertNotEquals(MemoryUtil.NULL, MemoryUtil.memAddress(buffer));
    }

    @Test
    void readsAnAllocationBackThroughItsAddress() {
        ByteBuffer buffer = MemoryUtil.memAlloc(4);
        buffer.put(2, (byte) 42);

        assertEquals((byte) 42, MemoryUtil.memByteBuffer(MemoryUtil.memAddress(buffer), 4).get(2));
    }

    @Test
    void hasNoAddressForNothingAndReadsNoneBack() {
        assertEquals(MemoryUtil.NULL, MemoryUtil.memAddressSafe((ByteBuffer) null));
        assertNull(MemoryUtil.memByteBufferSafe(MemoryUtil.NULL, 4));
        assertNull(MemoryUtil.memUTF8Safe(MemoryUtil.NULL));
    }

    @Test
    void roundTripsTextThroughAnAddress() {
        ByteBuffer encoded = MemoryUtil.memUTF8("flagon");

        assertEquals("flagon", MemoryUtil.memUTF8(MemoryUtil.memAddress(encoded)));
        assertEquals("flagon", MemoryUtil.memASCII(MemoryUtil.memAddress(MemoryUtil.memASCII("flagon"))));
    }

    @Test
    void allocatesTypedBuffersFromAStackFrame() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            FloatBuffer values = stack.floats(1.5f, 2.5f);

            width.put(0, 640);
            assertEquals(640, width.get(0));
            assertEquals(2.5f, values.get(1));
        }
    }

    @Test
    void givesTheFrameItsSpaceBackWhenItCloses() {
        MemoryStack stack = MemoryStack.stackGet();
        int before = stack.getPointer();

        try (MemoryStack frame = MemoryStack.stackPush()) {
            frame.malloc(128);
            assertNotEquals(before, frame.getPointer());
        }

        assertEquals(before, stack.getPointer());
    }

    @Test
    void alignsATypedAllocation() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            stack.malloc(1);

            assertEquals(0, MemoryUtil.memAddress(stack.mallocInt(1)) % Integer.BYTES);
        }
    }

    @Test
    void refusesAnAllocationLargerThanTheStack() {
        try (MemoryStack stack = MemoryStack.create(64).push()) {
            assertThrows(OutOfMemoryError.class, () -> stack.malloc(128));
        }
    }

    @Test
    void carriesAddressesInAPointerBuffer() {
        ByteBuffer target = MemoryUtil.memAlloc(4);
        target.put(0, (byte) 5);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointers = stack.pointers(MemoryUtil.memAddress(target));

            assertEquals((byte) 5, pointers.getByteBuffer(0, 4).get(0));
        }
    }
}
