package io.github.clamentos.gattoslab.scheduling;

///
import io.github.clamentos.gattoslab.exceptions.CauseContainer;
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
    private static final String SOURCE_DECODE = "SimpleCron.decodePeriod";

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

            scheduling uncertainty: +- 200ms (depends how fast the scheduler thread spins)
        */

        period = decodePeriod(simpleCron);
        this.task = task;
        this.name = name;

        final long now = System.currentTimeMillis();
        nextTrigger = now + period - (now % period);
    }

    ///..
    public Thread trigger(final long timestamp, final long[] idRef, final Map<Long, Thread> workers) {

        if(timestamp >= nextTrigger) {

            final long id = idRef[0];
            nextTrigger += period;

            final Thread worker = ThreadSpawner.createVirtualThread("gattos-lab-bsw-" + id + "-" + name, () -> {

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

    ///.
    public static long decodePeriod(final String simpleCron) throws IllegalArgumentException {

        if(simpleCron.length() >= 2) {

            final char unit = simpleCron.charAt(0);

            final long amount = Long.parseLong(simpleCron.substring(1));
            if(amount <= 0) throw new IllegalArgumentException("Amount must be greater than 0", new CauseContainer(SOURCE_DECODE));

            switch(unit) {

                case 's': return amount * 1000;
                case 'm': return amount * 1000 * 60;
                case 'h': return amount * 1000 * 60 * 60;

                default: throw new IllegalArgumentException("Unknown time unit '" + unit + "'", new CauseContainer(SOURCE_DECODE));
            }
        }

        else {

            throw new IllegalArgumentException("Malformed cron expression '" + simpleCron + "'", new CauseContainer(SOURCE_DECODE));
        }
    }

    ///
}
