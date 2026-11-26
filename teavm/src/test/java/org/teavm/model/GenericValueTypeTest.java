package org.teavm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GenericValueTypeTest {

    private static final GenericValueType.Argument[] NONE = new GenericValueType.Argument[0];

    @Test
    void namesAnInnerClassByItsFullName() {
        var outer = new GenericValueType.Object(null, "a.Outer", NONE);
        var inner = new GenericValueType.Object(outer, "Inner", NONE);
        assertTrue(inner.canBeRepresentedAsRaw());
        assertEquals(ValueType.object("a.Outer$Inner"), inner.asValueType());
    }

    @Test
    void keepsTheArgumentsOfAGenericOwner() {
        var variable = GenericValueType.Argument.invariant(new GenericValueType.Variable("K"));
        var outer = new GenericValueType.Object(null, "a.Outer",
                new GenericValueType.Argument[] { variable });
        assertFalse(new GenericValueType.Object(outer, "Inner", NONE).canBeRepresentedAsRaw());
    }
}
