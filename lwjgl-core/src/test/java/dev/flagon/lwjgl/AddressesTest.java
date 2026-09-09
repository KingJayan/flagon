package dev.flagon.lwjgl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flagon.lwjgl.internal.Addresses;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.junit.jupiter.api.Test;

class AddressesTest {

    private static ByteBuffer bytes(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    @Test
    void givesOneBufferTheSameAddressEveryTime() {
        ByteBuffer buffer = bytes(16);

        assertEquals(Addresses.of(buffer), Addresses.of(buffer));
    }

    @Test
    void givesTwoBuffersDifferentAddresses() {
        assertNotEquals(Addresses.of(bytes(16)), Addresses.of(bytes(16)));
    }

    @Test
    void hasNoAddressForNothing() {
        assertEquals(Addresses.NULL, Addresses.of(null));
    }

    @Test
    void countsFromThePosition() {
        ByteBuffer buffer = bytes(16);
        long start = Addresses.of(buffer);
        buffer.position(4);

        assertEquals(start + 4, Addresses.of(buffer));
    }

    @Test
    void countsAnIndexInElements() {
        IntBuffer buffer = bytes(16).asIntBuffer();

        assertEquals(Addresses.of(buffer, 0) + 8, Addresses.of(buffer, 2));
    }

    @Test
    void readsTheBufferBackFromItsAddress() {
        ByteBuffer buffer = bytes(4);
        buffer.put(0, (byte) 7);

        assertEquals((byte) 7, Addresses.bytes(Addresses.of(buffer), 4).get(0));
    }

    @Test
    void readsBackFromAnOffsetAddress() {
        ByteBuffer buffer = bytes(8);
        buffer.put(5, (byte) 9);

        assertEquals((byte) 9, Addresses.bytes(Addresses.of(buffer, 4), 4).get(1));
    }

    @Test
    void refusesToReadPastTheEnd() {
        long address = Addresses.of(bytes(4));

        assertThrows(IllegalArgumentException.class, () -> Addresses.bytes(address, 8));
    }

    @Test
    void refusesAnAddressItNeverHandedOut() {
        assertFalse(Addresses.isKnown(0xDEAD_BEEFL << 32));
        assertThrows(IllegalArgumentException.class,
                () -> Addresses.bytes(0xDEAD_BEEFL << 32, 4));
    }

    @Test
    void refusesTheNullAddress() {
        assertThrows(NullPointerException.class, () -> Addresses.bytes(Addresses.NULL, 4));
    }

    @Test
    void forgetsWhatWasRegisteredSinceAMark() {
        int mark = Addresses.mark();
        long address = Addresses.of(bytes(4));
        assertTrue(Addresses.isKnown(address));

        Addresses.reset(mark);

        assertFalse(Addresses.isKnown(address));
    }
}
