package io.github.clamentos.gattoslab.utils;

import java.util.concurrent.ThreadFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ThreadSpawner {

    public static Thread spawnVirtualThread(final String name, final Runnable task) {

        return Thread.ofVirtual().name(name).start(task);
    }

    public static Thread createVirtualThread(final String name, final Runnable task) {

        return Thread.ofVirtual().name(name).unstarted(task);
    }

    public static ThreadFactory threadFactoryFactory(final boolean isVirtual, final String prefix) {

        return isVirtual ? Thread.ofVirtual().name(prefix, 0).factory() : Thread.ofPlatform().name(prefix, 0).factory();
    }
}
