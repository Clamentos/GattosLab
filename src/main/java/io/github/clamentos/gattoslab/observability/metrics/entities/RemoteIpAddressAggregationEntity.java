package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.components.Streamable;
import io.github.clamentos.gattoslab.http.server.StreamWriter;

///..
import java.io.IOException;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class RemoteIpAddressAggregationEntity implements Streamable {

    ///
    private final String remoteIpAddress;

    ///..
    private long lastSeen;
    private int numberOfCalls;

    ///
    public RemoteIpAddressAggregationEntity(final String remoteIpAddress) {

        this.remoteIpAddress = remoteIpAddress;
    }

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write(String.valueOf(this.remoteIpAddress));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Integer.toString(this.numberOfCalls));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.lastSeen));
    }

    ///
}
