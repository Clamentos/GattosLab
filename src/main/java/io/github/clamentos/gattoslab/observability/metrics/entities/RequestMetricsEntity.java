package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.Resettable;
import io.github.clamentos.gattoslab.http.server.StreamWriter;
import io.github.clamentos.gattoslab.observability.Entity;

///..
import java.io.IOException;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class RequestMetricsEntity implements Resettable, Entity {

    ///
    private long id;
    private long timestamp;
    private int latency;
    private String uri;
    private String userAgent;
    private boolean isUnknown;
    private short httpStatus;

    ///..
    @Override
    public void reset() {

        this.uri = null;
        this.userAgent = null;
    }

    ///..
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write(Long.toString(this.id));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.timestamp));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Integer.toString(this.latency));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(String.valueOf(this.uri));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(String.valueOf(this.userAgent));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Boolean.toString(this.isUnknown));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Short.toString(this.httpStatus));
    }

    ///
}
