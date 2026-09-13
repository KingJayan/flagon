package java.nio.channels.spi;

import java.nio.channels.Selector;

public abstract class AbstractSelector extends Selector {

    private final SelectorProvider provider;

    protected AbstractSelector(SelectorProvider provider) {
        this.provider = provider;
    }

    public final SelectorProvider provider() {
        return provider;
    }
}
