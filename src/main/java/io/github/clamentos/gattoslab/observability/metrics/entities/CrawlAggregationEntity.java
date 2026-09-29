package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.components.Streamable;
import io.github.clamentos.gattoslab.http.server.StreamWriter;

///..
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class CrawlAggregationEntity implements Streamable {

    ///
    private final String path;
    private final boolean isUnknown;
    private final Set<String> statuses;

    ///..
    private long lastCalled;
    private int numberOfCalls;

    ///
    public CrawlAggregationEntity(final String path, final boolean isUnknown) {

        this.path = path;
        this.isUnknown = isUnknown;
        this.statuses = new TreeSet<>();
    }

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write(String.valueOf(this.path));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Boolean.toString(this.isUnknown));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.lastCalled));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Integer.toString(this.numberOfCalls));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);

        int remaining = statuses.size() - 1;

        for(final String status : this.statuses) {

            writer.write(status);
            if(remaining-- > 0) writer.write(ApplicationProperties.ARRAY_SEPARATOR);
        }
    }

    ///
}
