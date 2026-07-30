package dev.flagon.platform.browser;

import dev.flagon.platform.Clock;
import org.teavm.jso.JSBody;

public final class BrowserClock implements Clock {

    private long lastNanos;

    @JSBody(script = "return Date.now();")
    private static native double dateNow();

    @JSBody(script = "return performance.now();")
    private static native double performanceNow();

    @Override
    public long currentTimeMillis() {
        return (long) dateNow();
    }

    // shim: browsers coarsen and jitter performance.now(), so readings can arrive out of order.
    // desktop code assumes nanoTime() never goes backwards, so clamp instead.
    @Override
    public long nanoTime() {
        long nanos = (long) (performanceNow() * 1_000_000.0d);
        if (nanos < lastNanos) {
            return lastNanos;
        }
        lastNanos = nanos;
        return nanos;
    }
}
