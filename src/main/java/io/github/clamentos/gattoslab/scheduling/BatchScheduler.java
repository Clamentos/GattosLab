package io.github.clamentos.gattoslab.scheduling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.BatchConfig;
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

    ///
    public BatchScheduler(final ApplicationProperties applicationProperties) {

        final BatchConfig batchConfig = applicationProperties.getBatchConfig();

        shutdownTimeout = batchConfig.getShutdownTimeout();
        scheduler = ThreadSpawner.spawnVirtualThread("gattos-lab-bs-scheduler", this::triggerJobs);
        jobs = new CopyOnWriteArrayList<>();
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

            scheduler.interrupt();
            scheduler.join(shutdownTimeout);
        }

        catch(final InterruptedException _) {

            log.warn("Interrupted wile joining");
            Thread.currentThread().interrupt();
        }

        log.info("End shutdown");
    }

    ///.
    private final void triggerJobs() {

        final long[] idRef = new long[]{0};

        while(true) {

            final long now = System.currentTimeMillis();
            for(final SimpleCron job : jobs) job.trigger(now, idRef, workers);

            try {

                Thread.sleep(500L);
            }

            catch(final InterruptedException _) {

                for(final Thread worker : workers.values()) worker.interrupt();

                for(final Thread worker : workers.values()) {

                    try {

                        worker.join(shutdownTimeout);
                    }

                    catch(final InterruptedException _) {

                        log.error("Interrupted wile joining, force quitting");
                        Thread.currentThread().interrupt();

                        break;
                    }
                }

                break;
            }
        }
    }

    ///
}
