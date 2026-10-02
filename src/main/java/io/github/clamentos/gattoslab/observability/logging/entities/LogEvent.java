package io.github.clamentos.gattoslab.observability.logging.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.Resettable;
import io.github.clamentos.gattoslab.http.server.StreamWriter;
import io.github.clamentos.gattoslab.observability.Entity;

///..
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class LogEvent implements Resettable, Entity {

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
    public void reset() {

        this.severity = null;
        this.thread = null;
        this.logger = null;
        this.message = null;
        this.exception = null;
    }

    ///..
    @Override
    public void stream(final StreamWriter writer) throws IOException {

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

        writer.write(Long.toString(this.id));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.timestamp));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(String.valueOf(this.severity));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(String.valueOf(this.thread));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(String.valueOf(this.logger));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.writeForObservability(this.message);
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.writeForObservability(exceptionString);
    }

    ///
}
