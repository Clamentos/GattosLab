package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.datastructures.Resettable;
import io.github.clamentos.gattoslab.observability.Printable;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class RequestMetricsEntity implements Printable, Resettable {

    ///
    private long id;
    private long timestamp;
    private int latency;
    private String path;
    private String userAgent;
    private boolean isUnknown;
    private short httpStatus;

    ///..
    @Override
    public void appendBytes(final FastAsciiJoiner joiner) {

        joiner.add(Long.toString(this.id));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.timestamp));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Integer.toString(this.latency));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(this.path);
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(this.userAgent);
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Boolean.toString(this.isUnknown));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Short.toString(this.httpStatus));
    }

    ///..
    @Override
    public void reset() {

        this.path = null;
        this.userAgent = null;
    }

    ///
}
