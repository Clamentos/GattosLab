package io.github.clamentos.gattoslab.observability.logging;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAtomicCounter;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

///
public class SquashingLogger implements Closeable {

    ///
    private final Logger logger;

    ///..
    private final Map<String, FastAtomicCounter> squashes;

    ///
    public SquashingLogger(final BatchScheduler batchScheduler) {

        this.logger = new Logger();
        this.squashes = new ConcurrentHashMap<>();

        batchScheduler.schedule(this::logTask, "gattos-lab-squashed-logs-task", ApplicationProperties.SQUASHING_LOGGER_LOG_CRON);
    }

    ///
    public void warning(final String key) {

        this.squashes.computeIfAbsent(key, _ -> new FastAtomicCounter()).increment();
    }

    ///..
    @Override
    public void close() {

        this.logger.info("Begin shutdown...");
        this.logTask();
        this.logger.info("End shutdown");
    }

    ///.
    private void logTask() {

        final Iterator<Entry<String, FastAtomicCounter>> iterator = this.squashes.entrySet().iterator();
        final MutableString mutableString = new MutableString(128);

        while(iterator.hasNext()) {

            final Entry<String, FastAtomicCounter> entry = iterator.next();
            final String messageTemplate = entry.getKey();
            final int messageTemplateLength = messageTemplate.length();

            iterator.remove();

            for(int i = 0; i < messageTemplateLength; i++) {

                final char currentChar = messageTemplate.charAt(i);

                if(currentChar == ApplicationProperties.LOG_SQUASH_COUNTS_CHAR) mutableString.append(Long.toString(entry.getValue().get()));
                else mutableString.append(currentChar);
            }

            this.logger.warning(mutableString.toString());
            mutableString.clear();
        }
    }

    ///
}
