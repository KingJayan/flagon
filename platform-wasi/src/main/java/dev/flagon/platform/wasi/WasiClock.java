package dev.flagon.platform.wasi;

import dev.flagon.platform.Clock;
import org.teavm.runtime.wasi.Wasi;

final class WasiClock implements Clock {

    @Override
    public long currentTimeMillis() {
        return (long) Wasi.currentTimeMillis();
    }

    @Override
    public long nanoTime() {
        return Wasi.nanoTime();
    }
}
