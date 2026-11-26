package org.teavm.runtime.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import org.junit.jupiter.api.Test;

class WasiMathTest {

    /** edge values, then random ones over every exponent */
    private static double[] inputs() {
        List<Double> values = new ArrayList<>(List.of(0.0, -0.0, 1.0, -1.0, 0.5, -0.5, 2.0, 1e-300,
                Double.MIN_VALUE, -Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE,
                -Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN,
                Math.PI, -Math.PI, Math.PI / 2, Math.PI / 4, 3 * Math.PI / 4, 1e22, 1e300, 709.78,
                -745.1, 0.6744, 2.4375, 1.1875, 0.4375, 1e-9, 3.0e5, 1.0e10, 2.0e6, 7.0e19));
        Random random = new Random(42);
        for (int i = 0; i < 20000; i++) {
            values.add(Double.longBitsToDouble(random.nextLong()));
            values.add((random.nextDouble() - 0.5) * Math.pow(2, random.nextInt(64) - 20));
        }
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static void same(String name, DoubleUnaryOperator mine, DoubleUnaryOperator jdk) {
        for (double x : inputs()) {
            assertEquals(Double.doubleToLongBits(jdk.applyAsDouble(x)),
                    Double.doubleToLongBits(mine.applyAsDouble(x)), () -> name + "(" + x + ")");
        }
    }

    private static void same(String name, DoubleBinaryOperator mine, DoubleBinaryOperator jdk) {
        double[] values = inputs();
        Random random = new Random(7);
        for (int i = 0; i < 40000; i++) {
            double x = values[random.nextInt(values.length)];
            double y = i % 3 == 0 ? Math.rint(values[random.nextInt(60)] * 8) / 4
                    : values[random.nextInt(values.length)];
            assertEquals(Double.doubleToLongBits(jdk.applyAsDouble(x, y)),
                    Double.doubleToLongBits(mine.applyAsDouble(x, y)),
                    () -> name + "(" + x + ", " + y + ")");
        }
    }

    @Test
    void matchesStrictMath() {
        same("sin", WasiMath::sin, StrictMath::sin);
        same("cos", WasiMath::cos, StrictMath::cos);
        same("tan", WasiMath::tan, StrictMath::tan);
        same("asin", WasiMath::asin, StrictMath::asin);
        same("acos", WasiMath::acos, StrictMath::acos);
        same("atan", WasiMath::atan, StrictMath::atan);
        same("exp", WasiMath::exp, StrictMath::exp);
        same("log", WasiMath::log, StrictMath::log);
        same("sqrt", WasiMath::sqrt, StrictMath::sqrt);
        same("floor", WasiMath::floor, StrictMath::floor);
        same("ceil", WasiMath::ceil, StrictMath::ceil);
        same("atan2", WasiMath::atan2, StrictMath::atan2);
        same("pow", WasiMath::pow, StrictMath::pow);
    }
}
