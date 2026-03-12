package io.github.clamentos.gattoslab.scheduling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.BatchConfig;
import io.github.clamentos.gattoslab.utils.ThreadSpawner;

///..
import java.io.Closeable;
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
    private final long shutdownTimeout;

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
    public SimpleCron schedule(final Runnable task, final String name, final String simpleCron) throws IllegalArgumentException {

        final SimpleCron cron = new SimpleCron(task, simpleCron);

        log.info("Scheduled task: {}, period: {}ms", name, cron.getPeriod());
        return cron;
    }

    ///..
    @Override
    public void close() {

        try {

            scheduler.interrupt();
            scheduler.join(shutdownTimeout);
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            log.warn("Interrupted wile joining");
        }
    }

    ///.
    private final void triggerJobs() {

        long id = 0;

        while(true) {

            final long now = System.currentTimeMillis();
            for(final SimpleCron job : jobs) job.trigger(now, id++, workers);

            try {

                Thread.sleep(250);
            }

            catch(final InterruptedException _) {

                Thread.currentThread().interrupt();
                for(final Thread worker : workers.values()) worker.interrupt();

                for(final Thread worker : workers.values()) {

                    try {

                        worker.join(shutdownTimeout);
                    }

                    catch(final InterruptedException _) {

                        Thread.currentThread().interrupt();
                        log.warn("Interrupted wile joining");

                        break;
                    }
                }

                break;
            }
        }
    }

    ///
}
