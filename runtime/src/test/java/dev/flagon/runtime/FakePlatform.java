package dev.flagon.runtime;

import dev.flagon.platform.Clock;
import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platform;
import dev.flagon.platform.RuntimeHost;
import dev.flagon.platform.audio.AudioDevice;
import dev.flagon.platform.audio.SilentAudioDevice;
import dev.flagon.platform.net.MemoryNetwork;
import dev.flagon.platform.net.Network;
import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.display.DisplaySurface;
import dev.flagon.platform.fs.FileStore;
import dev.flagon.platform.fs.MemoryFileStore;

import java.util.ArrayList;
import java.util.List;

/** In-memory {@link Platform} that records everything the runtime reports. */
final class FakePlatform implements Platform, Clock, ConsoleSink, RuntimeHost {

    final List<String> events = new ArrayList<>();
    final List<String> messages = new ArrayList<>();
    Throwable fatalError;

    private final FileStore files = new MemoryFileStore();
    private final AudioDevice audio = new SilentAudioDevice(this);
    private final MemoryNetwork network = new MemoryNetwork();
    private final String[] arguments;
    private long nanos;

    FakePlatform(String... arguments) {
        this.arguments = arguments;
    }

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public Clock clock() {
        return this;
    }

    @Override
    public ConsoleSink console() {
        return this;
    }

    @Override
    public FileStore files() {
        return files;
    }

    @Override
    public AudioDevice audio() {
        return audio;
    }

    @Override
    public Network network() {
        return network;
    }

    @Override
    public RuntimeHost host() {
        return this;
    }

    @Override
    public boolean hasDisplay() {
        return false;
    }

    @Override
    public DisplaySurface display(int width, int height, String title) {
        throw new UnsupportedCapabilityException("the test host is headless");
    }

    @Override
    public long currentTimeMillis() {
        return 1_700_000_000_000L;
    }

    @Override
    public long nanoTime() {
        return nanos += 1_000L;
    }

    @Override
    public void write(Level level, String message, Throwable error) {
        messages.add(level + ": " + message);
    }

    @Override
    public String[] arguments() {
        return arguments;
    }

    @Override
    public void catalog(String[] ids, String[] descriptions) {
        for (int index = 0; index < ids.length; index++) {
            events.add("catalog:" + ids[index] + ":" + descriptions[index]);
        }
    }

    @Override
    public void status(String phase, String detail) {
        events.add("status:" + phase + ":" + detail);
    }

    @Override
    public void ready() {
        events.add("ready");
    }

    @Override
    public void exited(int code) {
        events.add("exited:" + code);
    }

    @Override
    public void fatal(String phase, Throwable error) {
        events.add("fatal:" + phase);
        fatalError = error;
    }
}
