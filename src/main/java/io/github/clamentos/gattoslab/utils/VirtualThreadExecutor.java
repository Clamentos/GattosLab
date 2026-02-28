package io.github.clamentos.gattoslab.utils;

///
import java.util.concurrent.Executor;

///
public final class VirtualThreadExecutor implements Executor {

    ///
    private final String prefix;
    private final FastAtomicCounter index;

    ///
    public VirtualThreadExecutor(final String prefix) {

        this.prefix = prefix;
        index = new FastAtomicCounter();
    }

    ///
    @Override
    public void execute(final Runnable command) {

        ThreadSpawner.spawnVirtualThread(prefix + index.get(), command);
    }

    ///
}
