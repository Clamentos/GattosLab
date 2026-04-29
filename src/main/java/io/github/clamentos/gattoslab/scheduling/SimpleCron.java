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

            scheduling uncertainty: +- 200ms (depends how fast the scheduler thread spins)
        */

        if(task == null || name == null) throw new IllegalArgumentException("SimpleCron.<init>~Parameters task and name cannot be null");

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

            final Thread worker = ThreadSpawner.createVirtualThread("gattos-lab-bsw-" + id + ">>" + name, () -> {

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

        if(simpleCron == null) throw new IllegalArgumentException("SimpleCron.decodePeriod~Cron expression cannot be null");

        if(simpleCron.length() >= 2) {

            final char unit = simpleCron.charAt(0);

            final long amount = Long.parseLong(simpleCron.substring(1));
            if(amount <= 0) throw new IllegalArgumentException("SimpleCron.decodePeriod~Amount must be greater than 0");

            switch(unit) {

                case 's': return amount * 1000;
                case 'm': return amount * 1000 * 60;
                case 'h': return amount * 1000 * 60 * 60;

                default: throw new IllegalArgumentException("SimpleCron.decodePeriod~Unknown time unit: " + unit);
            }
        }

        else {

            throw new IllegalArgumentException("SimpleCron.decodePeriod~Malformed cron expression: " + simpleCron);
        }
    }

    ///
}
