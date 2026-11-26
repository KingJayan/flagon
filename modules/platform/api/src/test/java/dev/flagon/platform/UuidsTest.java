package dev.flagon.platform;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UuidsTest {

    @Test
    void readsATextTheWayTheJdkDoes() {
        for (String text : new String[] {
            "123e4567-e89b-12d3-a456-426614174000", "0-0-0-0-0", "ABCDEF01-2-3-4-5"}) {
            UUID read = Uuids.fromString(text);
            assertEquals(UUID.fromString(text).toString(), read.toString(), text);
            assertEquals(UUID.fromString(text).getMostSignificantBits(),
                    Uuids.getMostSignificantBits(read), text);
        }
    }

    /** a launcher run without --uuid hands the game its player name, which it then tries */
    @Test
    void refusesWhatIsNotAUuid() {
        for (String text : new String[] {
            "Player456", "1-2-3-4", "1-2-3-4-5-6", "1--3-4-5", "g-0-0-0-0"}) {
            assertThrows(IllegalArgumentException.class, () -> Uuids.fromString(text), text);
        }
    }
}
