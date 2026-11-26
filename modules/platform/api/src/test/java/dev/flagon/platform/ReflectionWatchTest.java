package dev.flagon.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * on a jvm nothing is stripped, so the watch has to hand back what Class itself would.
 */
class ReflectionWatchTest {

    @SuppressWarnings("unused")
    private static final class Sample {
        private int count;

        private Sample() {
        }

        void step() {
            count++;
        }
    }

    @Test
    void theDeclaredMembersAreTheOnesClassReports() {
        assertEquals(Sample.class.getDeclaredMethods().length,
                ReflectionWatch.declaredMethods(Sample.class).length);
        assertEquals(Sample.class.getDeclaredFields().length,
                ReflectionWatch.declaredFields(Sample.class).length);
        assertEquals(Sample.class.getDeclaredConstructors().length,
                ReflectionWatch.declaredConstructors(Sample.class).length);
    }

    @Test
    void thePublicMembersAreTheOnesClassReports() {
        assertEquals(String.class.getMethods().length,
                ReflectionWatch.methods(String.class).length);
        assertEquals(String.class.getFields().length, ReflectionWatch.fields(String.class).length);
        assertEquals(String.class.getConstructors().length,
                ReflectionWatch.constructors(String.class).length);
    }

    /** an interface has no constructor either, so it must not be mistaken for a stripped class */
    @Test
    void anEmptyInterfaceIsNotReportedAsStripped() {
        assertEquals(0, ReflectionWatch.declaredMethods(Marker.class).length);
        assertTrue(Marker.class.isInterface());
    }

    interface Marker {
    }
}
