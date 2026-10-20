package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.address.IpAddress;
import io.github.clamentos.gattoslab.datastructures.interfaces.Entity;
import io.github.clamentos.gattoslab.datastructures.interfaces.Resettable;

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
    private boolean isUserAgentBlocked;
    private IpAddress remoteIpAddress;

    ///..
    @Override
    public void reset() {

        this.uri = null;
        this.userAgent = null;
        this.remoteIpAddress = null;
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
        writer.writeForObservability(this.uri);
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.writeForObservability(String.valueOf(this.userAgent));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Boolean.toString(this.isUnknown));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Short.toString(this.httpStatus));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Boolean.toString(this.isUserAgentBlocked));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);

        if(this.remoteIpAddress != null) this.remoteIpAddress.stream(writer);
    }

    ///
}
