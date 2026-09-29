package io.github.clamentos.gattoslab.observability.logging;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.Siphon;
import io.github.clamentos.gattoslab.observability.ObservabilityFile;
import io.github.clamentos.gattoslab.observability.logging.entities.LogEvent;
import io.github.clamentos.gattoslab.observability.logging.entities.LogSeverity;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

///..
import lombok.Getter;

///
@SuppressWarnings("java:S106")

///
public final class LoggerRoot implements Closeable {

    ///
    private static final LoggerRoot INSTANCE = new LoggerRoot();

    ///.
    @Getter
    private final ObservabilityFile<LogEvent> logFile;

    private final AtomicLong logEventIdCounter;
    private final Siphon<LogEvent> primarySiphon;
    private final Siphon<LogEvent> secondarySiphon;
    private final AtomicReference<Siphon<LogEvent>> currentSiphonReference;

    ///..
    private final Thread drainWorker;

    ///
    private LoggerRoot() throws IllegalStateException {

        try {

            this.logFile = new ObservabilityFile<>(ApplicationProperties.LOG_FILE_PATH, null);

            this.logEventIdCounter = new AtomicLong();
            this.primarySiphon = new Siphon<>(LogEvent.class, ApplicationProperties.LOG_SIPHON_CAPACITY, LogEvent::new, this::drainSiphonTask);
            this.secondarySiphon = new Siphon<>(LogEvent.class, ApplicationProperties.LOG_SIPHON_CAPACITY, LogEvent::new, this::drainSiphonTask);
            this.currentSiphonReference = new AtomicReference<>(this.primarySiphon);

            this.drainWorker = GenericUtils.spawnVirtualThread("gattos-lab-log-siphon-drain-task", this::forceDrainSiphonTask);
        }

        catch(final IOException exc) {

            System.err.println("FATAL: Could not instantiate LoggerRoot because: " + exc);
            throw new IllegalStateException(exc);
        }
    }

    ///
    public static LoggerRoot getInstance() {

        return INSTANCE;
    }

    ///
    public void info(final String logger, final String message) {

        this.log(LogSeverity.INFO, logger, message, null);
    }

    ///..
    public void warn(final String logger, final String message,final Throwable exception) {

        this.log(LogSeverity.WARNING, logger, message, exception);
    }

    ///..
    public void error(final String logger, final String message, final Throwable exception) {

        this.log(LogSeverity.ERROR, logger, message, exception);
    }

    ///..
    @Override
    public void close() {

        try {

            this.drainWorker.interrupt();
            if(!this.drainWorker.join(ApplicationProperties.LOG_CLOSE_TIMEOUT)) System.out.println("Timed-out while joining the 'drainWorker'");
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
        }
    }

    ///.
    private void log(final LogSeverity logSeverity, final String logger, final String message, final Throwable exception) {

        while(!(this.currentSiphonReference.get().update(entity -> this.updateLogEvent(entity, logSeverity, logger, message, exception)))) {

            GenericUtils.silentSleepNanos(100_000);
        }
    }

    ///..
    private void updateLogEvent(

        final LogEvent event,
        final LogSeverity logSeverity,
        final String logger,
        final String message,
        final Throwable exception
    ) {

        event.setId(this.logEventIdCounter.getAndIncrement());
        event.setTimestamp(System.currentTimeMillis());
        event.setSeverity(logSeverity);
        event.setThread(Thread.currentThread().getName());
        event.setLogger(logger);
        event.setMessage(message);
        event.setException(exception);
    }

    ///..
    private void drainSiphonTask(final List<LogEvent> logEvents) {

        if(!logEvents.isEmpty()) {

            final Siphon<LogEvent> previousSiphon;

            if(this.currentSiphonReference.compareAndSet(this.primarySiphon, this.secondarySiphon)) {

                previousSiphon = this.primarySiphon;
            }

            else {

                this.currentSiphonReference.set(this.primarySiphon);
                previousSiphon = this.secondarySiphon;
            }

            while(previousSiphon.isUpdating()) {

                GenericUtils.silentSleepNanos(100_000);
            }

            try {

                this.logFile.write(logEvents);
            }

            catch(final IOException exc) {

                this.error(LoggerRoot.class.getSimpleName() + ".drainSiphonTask", "Could not write to file", exc);
            }
        }
    }

    ///..
    private void forceDrainSiphonTask() {

        final long sleepAmount = ApplicationProperties.LOG_SIPHON_DRAIN_TASK_SLEEP.toMillis();
        final long now = System.currentTimeMillis();

        GenericUtils.silentSleep(sleepAmount - (now % sleepAmount));

        while(true) {

            this.currentSiphonReference.get().drain();
            if(Thread.currentThread().isInterrupted()) break;
            GenericUtils.silentSleep(sleepAmount);
        }

        this.currentSiphonReference.get().drain();
    }

    ///
}
