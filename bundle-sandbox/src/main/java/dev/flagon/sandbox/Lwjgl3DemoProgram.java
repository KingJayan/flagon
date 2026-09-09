package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** exercises the lwjgl 3 surface in the browser */
public final class Lwjgl3DemoProgram implements Program {

    @Override
    public String id() {
        return "lwjgl3";
    }

    @Override
    public String description() {
        return "runs the lwjgl 3 memory model";
    }

    @Override
    public void run(String[] arguments) {
        ConsoleSink console = Platforms.get().console();
        addresses(console);
        stack(console);
    }

    private void addresses(ConsoleSink console) {
        ByteBuffer buffer = MemoryUtil.memAlloc(8);
        buffer.put(3, (byte) 42);
        long address = MemoryUtil.memAddress(buffer);

        console.info("lwjgl3: address reads back " + MemoryUtil.memByteBuffer(address, 8).get(3));
        console.info("lwjgl3: address plus four reads back "
                + MemoryUtil.memByteBuffer(address + 4, 4).get(0));
        console.info("lwjgl3: nothing has address " + MemoryUtil.memAddressSafe((ByteBuffer) null));

        try {
            MemoryUtil.memByteBuffer(0x7FFF_0000_0000L, 4);
            console.info("lwjgl3: an invented address was followed");
        } catch (IllegalArgumentException refused) {
            console.info("lwjgl3: an invented address is refused");
        }

        console.info("lwjgl3: text round trips as "
                + MemoryUtil.memUTF8(MemoryUtil.memAddress(MemoryUtil.memUTF8("flagon"))));
    }

    private void stack(ConsoleSink console) {
        MemoryStack shared = MemoryStack.stackGet();
        int before = shared.getPointer();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            width.put(0, 854);
            height.put(0, 480);

            PointerBuffer pointers = stack.pointers(MemoryUtil.memAddress(width));
            console.info("lwjgl3: the stack holds " + width.get(0) + "x" + height.get(0)
                    + ", reached again as " + pointers.getByteBuffer(0, 4).asIntBuffer().get(0));
        }

        console.info("lwjgl3: the frame gave back " + (shared.getPointer() == before
                ? "every byte" : "some of its bytes"));
    }
}
