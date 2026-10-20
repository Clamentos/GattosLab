package io.github.clamentos.gattoslab.observability.logging;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAtomicCounter;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.Closeable;
import java.lang.StackWalker.StackFrame;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

///
public class SquashingLogger implements Closeable {

    ///
    private final Logger logger;

    ///..
    private final Map<Pair<String, String>, Pair<FastAtomicCounter, Long>> events;
    private final DateTimeFormatter dateTimeFormatter;

    ///
    public SquashingLogger(final BatchScheduler batchScheduler) {

        this.logger = new Logger();

        this.events = new ConcurrentHashMap<>();
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS '['VV']'");

        batchScheduler.schedule(this::logTask, "gattos-lab-squashed-logs-task", ApplicationProperties.SQUASHING_LOGGER_LOG_CRON);
    }

    ///
    public void warning(final String key) {

        final Pair<FastAtomicCounter, Long> data = this.events.computeIfAbsent(

            new Pair<>(key, this.getCallerMethod()),
            _ -> new Pair<>(new FastAtomicCounter(), System.currentTimeMillis())
        );

        data.getA().increment();
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

        for(final Entry<Pair<String, String>, Pair<FastAtomicCounter, Long>> event : this.events.entrySet()) {

            final Pair<String, String> eventKey = event.getKey();
            final String messageTemplate = eventKey.getA();
            final int messageTemplateLength = messageTemplate.length();

            this.events.remove(eventKey);

            for(int i = 0; i < messageTemplateLength; i++) {

                final char currentChar = messageTemplate.charAt(i);

                if(currentChar == ApplicationProperties.LOG_SQUASH_COUNTS_CHAR) mutableString.append(Long.toString(event.getValue().getA().get()));
                else mutableString.append(currentChar);
            }

            final ZonedDateTime firstEventDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.getValue().getB()), GenericUtils.DEFAULT_ZONE_ID);

            mutableString.append(" (original date: ");
            mutableString.append(this.dateTimeFormatter.format(firstEventDate));
            mutableString.append(")");

            this.logger.warning(mutableString.toString(), eventKey.getB());
            mutableString.clear();
        }
    }

    ///
}
