package io.github.clamentos.gattoslab.eventbus;

///
import io.github.clamentos.gattoslab.utils.ThreadSpawner;

///..
import java.util.concurrent.atomic.AtomicLong;

///
public final class EventBus {

    ///
    private final AtomicLong workerIndex;
    private final Runnable command;

    ///
    public EventBus(final Runnable command) {

        workerIndex = new AtomicLong();
        this.command = command;
    }

    ///
    public void trigger() {

        ThreadSpawner.spawnVirtualThread("gattos-lab-eb-" + workerIndex.getAndIncrement(), command);
    }

    ///
}
