package dev.flagon.platform.wasi;

import dev.flagon.platform.RuntimeHost;

/** a command line has nothing to show phases on; the exit status is what {@code boot} returns */
final class WasiRuntimeHost implements RuntimeHost {

    private final String[] arguments;

    WasiRuntimeHost(String[] arguments) {
        this.arguments = arguments;
    }

    @Override
    public String[] arguments() {
        return arguments.clone();
    }

    @Override
    public void catalog(String[] ids, String[] descriptions) {
    }

    @Override
    public void status(String phase, String detail) {
    }

    @Override
    public void ready() {
    }

    @Override
    public void exited(int code) {
    }

    @Override
    public void fatal(String phase, Throwable error) {
    }
}
