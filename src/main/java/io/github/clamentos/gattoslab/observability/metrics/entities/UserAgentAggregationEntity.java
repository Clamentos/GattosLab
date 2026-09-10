package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.observability.Printable;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class UserAgentAggregationEntity implements Printable {

    ///
    private final String userAgent;

    ///..
    private long lastSeen;
    private int numberOfCalls;

    ///
    public UserAgentAggregationEntity(final String userAgent) {

        this.userAgent = userAgent;
    }

    ///
    @Override
    public void appendBytes(final FastAsciiJoiner joiner) {

        joiner.add(this.userAgent);
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.lastSeen));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Integer.toString(this.numberOfCalls));
    }

    ///
}
