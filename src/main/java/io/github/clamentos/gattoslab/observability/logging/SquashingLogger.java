package io.github.clamentos.gattoslab.observability.logging;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAtomicCounter;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.io.Closeable;
import java.lang.StackWalker.StackFrame;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

///
public class SquashingLogger implements Closeable {

    ///
    private final Logger logger;

    ///..
    private final Map<Pair<String, String>, FastAtomicCounter> events;

    ///
    public SquashingLogger(final BatchScheduler batchScheduler) {

        this.logger = new Logger();
        this.events = new ConcurrentHashMap<>();

        batchScheduler.schedule(this::logTask, "gattos-lab-squashed-logs-task", ApplicationProperties.SQUASHING_LOGGER_LOG_CRON);
    }

    ///
    public void warning(final String key) {

        this.events.computeIfAbsent(new Pair<>(key, this.getCallerMethod()), _ -> new FastAtomicCounter()).increment();
    }

    ///..
    @Override
    public void close() {

        this.logger.info("Begin shutdown...");
        this.logTask();
        this.logger.info("End shutdown");
    }

    ///.
    private String getCallerMethod() {

        return StackWalker.getInstance().walk(frames -> frames

            .skip(2)
            .findFirst()
            .map(StackFrame::getMethodName)
            .orElse(ApplicationProperties.UNKNOWN_METHOD_PLACEHOLDER)
        );
    }

    ///..
    private void logTask() {

        final MutableString mutableString = new MutableString(128);

        for(final Entry<Pair<String, String>, FastAtomicCounter> event : this.events.entrySet()) {

            final Pair<String, String> eventKey = event.getKey();
            final String messageTemplate = eventKey.getA();
            final int messageTemplateLength = messageTemplate.length();

            this.events.remove(eventKey);

            for(int i = 0; i < messageTemplateLength; i++) {

                final char currentChar = messageTemplate.charAt(i);

                if(currentChar == ApplicationProperties.LOG_SQUASH_COUNTS_CHAR) mutableString.append(Long.toString(event.getValue().get()));
                else mutableString.append(currentChar);
            }

            this.logger.warning(mutableString.toString(), eventKey.getB());
            mutableString.clear();
        }
    }

    ///
}
