package dev.flagon.lwjgl.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GlslWordsTest {

    @Test
    void replacesEveryWholeWord() {
        assertEquals("b b b", GlslTranslator.replaceWord("a a a", "a", "b"));
    }

    @Test
    void leavesAWordItOnlyLooksLikePartOf() {
        assertEquals("ab ba a_b b", GlslTranslator.replaceWord("ab ba a_b a", "a", "b"));
    }

    @Test
    void handsBackTheSameTextWhenTheWordIsAbsent() {
        String source = "void main() { }";

        assertSame(source, GlslTranslator.replaceWord(source, "a", "b"));
    }

    @Test
    void doesNotReplaceInsideWhatItJustWrote() {
        assertEquals("texture_x(uv)", GlslTranslator.replaceWord("texture(uv)", "texture",
                "texture_x"));
    }

    @Test
    void keepsTheTextAroundEveryOccurrence() {
        assertEquals("f(x); g(x); h(x);",
                GlslTranslator.replaceWord("f(v); g(v); h(v);", "v", "x"));
    }

    @Test
    void replacesAWordAtBothEnds() {
        assertEquals("x b x", GlslTranslator.replaceWord("a b a", "a", "x"));
    }
}
