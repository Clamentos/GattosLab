package io.github.clamentos.gattoslab.scheduling;

///
import io.github.clamentos.gattoslab.utils.ThreadSpawner;

///..
import java.util.Map;

///..
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class SimpleCron {

    ///
    private final Runnable task;
    private final String name;

    ///..
    @Getter
    private final long period;

    private long nextTrigger;

    ///
    public SimpleCron(final Runnable task, final String name, final String simpleCron) throws IllegalArgumentException {

        /*
            Very simple cron scheduling (no offsets): <time-unit><amount>

            time-units:

                s -> seconds
                m -> minutes
                h -> hours
        */

        if(simpleCron.length() >= 2) {

            final char unit = simpleCron.charAt(0);

            final long amount = Long.parseLong(simpleCron.substring(1));
            if(amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0");

            final long now = System.currentTimeMillis();

            switch(unit) {

                case 's': period = amount * 1000; break;
                case 'm': period = amount * 1000 * 60; break;
                case 'h': period = amount * 1000 * 60 * 60; break;

                default: throw new IllegalArgumentException("Unknown time unit: " + unit);
            }

            this.task = task;
            this.name = name;

            nextTrigger = System.currentTimeMillis() + period - (now % period);
        }

        else {

            throw new IllegalArgumentException("Malformed cron expression: " + simpleCron);
        }
    }

    ///..
    public Thread trigger(final long timestamp, final long[] idRef, final Map<Long, Thread> workers) {

        if(timestamp >= nextTrigger) {

            final long id = idRef[0];
            nextTrigger += period;

            final Thread worker = ThreadSpawner.createVirtualThread("gattos-lab-bsw-" + id, () -> {

                try { task.run(); }
                catch(final RuntimeException exc) { log.error("Uncaught exception in scheduled task {}", name, exc); }

                workers.remove(id);
            });

            workers.put(id, worker);
            worker.start();
            idRef[0]++;

            return worker;
        }

        return null;
    }

    ///
}
