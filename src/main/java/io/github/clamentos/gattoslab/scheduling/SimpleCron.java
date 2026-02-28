package io.github.clamentos.gattoslab.scheduling;

///
import lombok.Getter;

///
@Getter

///
public final class SimpleCron {

    ///
    private final long period;
    private final long initialDelay;

    ///
    public SimpleCron(final String simpleCron) throws IllegalArgumentException {

        /*
            Very simple cron scheduling (no offsets): <time-unit><amount>

            time-units:

                l -> milliseconds
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

                case 'l': period = amount; break;
                case 's': period = amount * 1000; break;
                case 'm': period = amount * 1000 * 60; break;
                case 'h': period = amount * 1000 * 60 * 60; break;

                default: throw new IllegalArgumentException("Unknown time unit: " + unit);
            }

            initialDelay = period - (now % period);
        }

        else {

            throw new IllegalArgumentException("Malformed cron expression: " + simpleCron);
        }
    }

    ///
}
