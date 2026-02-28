package io.github.clamentos.gattoslab.scheduling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.BatchConfig;
import io.github.clamentos.gattoslab.utils.ThreadSpawner;

///..
import java.io.Closeable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class BatchScheduler implements Closeable {

    ///
    private final long shutdownTimeout;

    ///..
    private final ScheduledThreadPoolExecutor executor;

    ///
    public BatchScheduler(final ApplicationProperties applicationProperties) {

        final BatchConfig batchConfig = applicationProperties.getBatchConfig();

        shutdownTimeout = batchConfig.getShutdownTimeout();
        executor = new ScheduledThreadPoolExecutor(batchConfig.getPoolSize(), ThreadSpawner.threadFactoryFactory(true, "gattos-lab-bs-worker"));
    }

    ///
    public SimpleCron schedule(final Runnable task, final String name, final String simpleCron) throws IllegalArgumentException {

        final SimpleCron cron = new SimpleCron(simpleCron);
        executor.scheduleAtFixedRate(task, cron.getInitialDelay(), cron.getPeriod(), TimeUnit.MILLISECONDS);

        log.info("Scheduled task: {}, initial delay: {}ms, period: {}ms", name, cron.getInitialDelay(), cron.getPeriod());
        return cron;
    }

    ///..
    @Override
    public void close() {

        executor.shutdown();

        try {

            if(!executor.awaitTermination(shutdownTimeout, TimeUnit.MILLISECONDS)) log.error("Could not close the executor due to timeout");
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            log.warn("Interrupted wile closing");
        }
    }

    ///
}
