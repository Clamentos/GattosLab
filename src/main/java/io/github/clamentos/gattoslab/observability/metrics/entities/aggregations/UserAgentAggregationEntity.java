package io.github.clamentos.gattoslab.observability.metrics.entities.aggregations;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.interfaces.Streamable;

///..
import java.io.IOException;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class UserAgentAggregationEntity implements Streamable {

    ///
    private final String userAgent;
    private final boolean isBlocked;

    ///..
    private long lastSeen;
    private int numberOfCalls;

    ///
    public UserAgentAggregationEntity(final String userAgent, final boolean isBlocked) {

        this.userAgent = userAgent;
        this.isBlocked = isBlocked;
    }

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.writeForObservability(String.valueOf(this.userAgent));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Integer.toString(this.numberOfCalls));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.lastSeen));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Boolean.toString(this.isBlocked));
    }

    ///
}
