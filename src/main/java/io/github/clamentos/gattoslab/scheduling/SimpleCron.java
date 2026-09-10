package io.github.clamentos.gattoslab.scheduling;

///
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.Map;

///..
import lombok.Getter;

///
public final class SimpleCron {

    ///
    private final Logger logger;

    ///..
    private final Runnable task;
    private final String name;

    ///..
    @Getter
    private final long period;

    private long nextTriggerTimestamp;

    ///
    public SimpleCron(final Logger logger, final String simpleCron, final Runnable task, final String name) throws IllegalArgumentException {

        this.logger = logger;

        this.period = this.decodePeriod(simpleCron);
        this.task = task;
        this.name = name;

        final long now = System.currentTimeMillis();
        this.nextTriggerTimestamp = now + this.period - (now % this.period);
    }

    ///..
    public void trigger(final long timestamp, final long[] idRef, final Map<Long, Thread> workers) {

        if(timestamp >= this.nextTriggerTimestamp) {

            final long id = idRef[0];
            this.nextTriggerTimestamp += this.period;

            final Thread worker = GenericUtils.createVirtualThread(this.name + "-" + id, () -> {

                try {

                    this.task.run();
                }

                catch(final RuntimeException exc) {

                    this.logger.error("Uncaught exception in scheduled task '" + this.name + "'", exc);
                }

                workers.remove(id);
            });

            workers.put(id, worker);
            worker.start();
            idRef[0]++;
        }
    }

    ///.
    private long decodePeriod(final String simpleCron) throws IllegalArgumentException {

        if(simpleCron.length() >= 2) {

            final char unit = simpleCron.charAt(0);
            final long amount = Long.parseLong(simpleCron.substring(1));

            if(amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");

            switch(unit) {

                case 's': return amount * 1000;
                case 'm': return amount * 1000 * 60;
                case 'h': return amount * 1000 * 60 * 60;

                default: throw new IllegalArgumentException("Unknown time unit '" + unit + "'");
            }
        }

        else {

            throw new IllegalArgumentException("Malformed cron expression '" + simpleCron + "'");
        }
    }

    ///
}
