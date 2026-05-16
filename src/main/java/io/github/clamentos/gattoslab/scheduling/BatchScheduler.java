package io.github.clamentos.gattoslab.scheduling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.ThreadSpawner;

///..
import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class BatchScheduler implements Closeable {

    ///
    private final Duration shutdownTimeout;

    ///..
    private final Thread scheduler;

    private final List<SimpleCron> jobs;
    private final Map<Long, Thread> workers;

    private volatile boolean halt = false;

    ///
    public BatchScheduler(final ApplicationProperties applicationProperties) {

        shutdownTimeout = applicationProperties.getBatchSchedulerShutdownTimeout();

        jobs = new CopyOnWriteArrayList<>();
        scheduler = ThreadSpawner.spawnVirtualThread("gattos-lab-bs", this::triggerJobs);

        workers = new ConcurrentHashMap<>();
    }

    ///
    public long schedule(final Runnable task, final String name, final String simpleCron) throws IllegalArgumentException {

        final SimpleCron cron = new SimpleCron(task, name, simpleCron);
        jobs.add(cron);

        log.info("Scheduled task: {}, period: {}ms", name, cron.getPeriod());
        return cron.getPeriod();
    }

    ///..
    @Override
    public void close() {

        log.info("Begin shutdown...");

        try {

            halt = true;
            if(!scheduler.join(shutdownTimeout.multipliedBy(Math.max(workers.size(), 1)))) log.warn("Timed-out while joining");
        }

        catch(final InterruptedException _) {

            log.error("Interrupted wile joining, force quitting");
            Thread.currentThread().interrupt();
        }

        log.info("End shutdown");
    }

    ///.
    private final void triggerJobs() {

        final long[] idRef = new long[]{0};

        while(!halt) {

            final long now = System.currentTimeMillis();
            for(final SimpleCron job : jobs) job.trigger(now, idRef, workers);

            GenericUtils.silentSleep(200L);
        }

        log.info("Exiting, joining {} workers", workers.size());

        for(final Thread worker : workers.values()) {

            try {

                log.info("Joining {}", worker.getName());
                if(!worker.join(shutdownTimeout)) log.warn("Timed-out while joining {}", worker.getName());
            }

            catch(final InterruptedException _) {

                log.error("Interrupted wile joining, force quitting");
                Thread.currentThread().interrupt();

                break;
            }
        }
    }

    ///
}
