package org.teavm.backend.javascript.codegen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DefaultAliasProviderTest {

    @Test
    void givesAClassNamedLikeAKeywordAnotherAlias() {
        var aliases = new DefaultAliasProvider(10_000);

        assertNotEquals("in", aliases.getClassAlias("in").name);
        assertNotEquals("do", aliases.getClassAlias("do").name);
    }

    @Test
    void keepsTheNameOfAnOrdinaryClass() {
        var aliases = new DefaultAliasProvider(10_000);

        assertEquals("ave", aliases.getClassAlias("ave").name);
    }
}
