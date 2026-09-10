package io.github.clamentos.gattoslab.observability.logging.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.datastructures.Resettable;
import io.github.clamentos.gattoslab.observability.Printable;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.PrintWriter;
import java.io.StringWriter;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class LogEvent implements Printable, Resettable {

    ///
    private long id;
    private long timestamp;
    private LogSeverity severity;
    private String thread;
    private String logger;
    private String message;
    private Throwable exception;

    ///
    @Override
    public void appendBytes(final FastAsciiJoiner joiner) {

        final String exceptionString;

        if(this.exception != null) {

            final StringWriter stringWriter = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(stringWriter);

            this.exception.printStackTrace(printWriter);
            exceptionString = stringWriter.toString();
        }

        else {

            exceptionString = "";
        }

        joiner.add(Long.toString(this.id));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.timestamp));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(String.valueOf(this.severity));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(this.thread);
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(this.logger);
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(GenericUtils.normalizedForObservability(this.message));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(GenericUtils.normalizedForObservability(exceptionString));
    }

    ///..
    @Override
    public void reset() {

        this.severity = null;
        this.thread = null;
        this.logger = null;
        this.message = null;
        this.exception = null;
    }

    ///
}
