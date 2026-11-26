package dev.flagon.platform;

import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkGapsTest {

    @Test
    void theMemoryCeilingIsANumberAProgramCanSizeACacheFrom() {
        long ceiling = Runtimes.maxMemory(Runtime.getRuntime());
        assertTrue(ceiling > 0, "a ceiling of " + ceiling + " sizes nothing");
        assertEquals(ceiling, Runtimes.maxMemory(Runtime.getRuntime()));
    }

    @Test
    void startingAProcessIsRefusedByName() {
        assertThrows(UnsupportedCapabilityException.class,
                () -> Runtimes.exec(Runtime.getRuntime(), "ls"));
        assertThrows(UnsupportedCapabilityException.class,
                () -> Runtimes.exec(Runtime.getRuntime(), new String[] {"ls"}));
    }

    @Test
    void loadingANativeLibraryIsRefusedByName() {
        UnsupportedCapabilityException refused = assertThrows(
                UnsupportedCapabilityException.class, () -> Systems.loadLibrary("lwjgl"));
        assertTrue(refused.getMessage().contains("lwjgl"), refused.getMessage());
        assertThrows(UnsupportedCapabilityException.class, () -> Systems.load("/lib/lwjgl.so"));
    }

    @Test
    void aGenericTypeAnswersThePlainClassBehindIt() {
        assertEquals(Object.class, Classes.getGenericSuperclass(String.class));
        assertTrue(List.of(Classes.getGenericInterfaces(String.class))
                .contains(Serializable.class));
    }

    @Test
    void nothingSignsABundle() {
        assertArrayEquals(new Object[0], Classes.getSigners(String.class));
    }

    @Test
    void anAnonymousClassIsTheOneWithANumberForAName() {
        Runnable anonymous = new Runnable() {
            @Override
            public void run() {
            }
        };
        assertTrue(Classes.isAnonymousClass(anonymous.getClass()),
                anonymous.getClass().getName());
        assertFalse(Classes.isAnonymousClass(String.class));
        assertFalse(Classes.isAnonymousClass(JdkGapsTest.class));
    }
}
