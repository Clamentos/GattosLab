package io.github.clamentos.gattoslab.scheduling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

///
public final class BatchScheduler implements Closeable {

    ///
    private final Logger logger;
    private final Logger cronLogger;

    ///..
    private final List<SimpleCron> jobs;
    private final Map<Long, Thread> workers;

    ///..
    private final Thread scheduler;
    private final AtomicBoolean isClosed;

    ///
    public BatchScheduler() {

        this.logger = new Logger();
        this.cronLogger = new Logger(SimpleCron.class.getSimpleName());

        this.jobs = new ArrayList<>();
        this.workers = new ConcurrentHashMap<>();

        this.isClosed = new AtomicBoolean();
        this.scheduler = GenericUtils.spawnVirtualThread("gattos-lab-batch-scheduler-task", this::triggerJobsTask);
    }

    ///
    public long schedule(final Runnable task, final String name, final String simpleCron) {

        final SimpleCron cron = new SimpleCron(this.cronLogger, simpleCron, task, name);
        final long period = cron.getPeriod();

        this.jobs.add(cron);
        this.logger.info("Scheduled task '" + name + "', period: " + period + "ms");

        return period;
    }

    ///..
    @Override
    public void close() {

        this.logger.info("Begin shutdown...");

        try {

            this.isClosed.set(true);

            final Duration timeout = ApplicationProperties.SCHEDULER_SHUTDOWN_TIMEOUT.multipliedBy(Math.max(this.workers.size(), 1));
            if(!this.scheduler.join(timeout)) this.logger.warning("Timed-out while joining");
        }

        catch(final InterruptedException _) {

            this.logger.error("Interrupted wile joining, force quitting");
            Thread.currentThread().interrupt();
        }

        this.logger.info("End shutdown");
    }

    ///.
    private final void triggerJobsTask() {

        final long sleep = ApplicationProperties.SCHEDULER_POLL_PERIOD.toMillis();
        final long[] idRef = new long[]{0};

        while(!this.isClosed.get()) {

            final long now = System.currentTimeMillis();
            final int length = this.jobs.size();

            for(int i = 0; i < length; i++) {

                this.jobs.get(i).trigger(now, idRef, this.workers);
            }

            GenericUtils.silentSleep(sleep);
        }

        this.logger.info("Exiting, joining " + this.workers.size() + " workers");

        for(final Thread worker : this.workers.values()) {

            try {

                final String name = worker.getName();

                this.logger.info("Joining '" + name + "'");
                if(!worker.join(ApplicationProperties.SCHEDULER_SHUTDOWN_TIMEOUT)) this.logger.warning("Timed-out while joining '" + name + "'");
            }

            catch(final InterruptedException _) {

                this.logger.error("Interrupted wile joining, force quitting");
                Thread.currentThread().interrupt();

                return;
            }
        }
    }

    ///
}
