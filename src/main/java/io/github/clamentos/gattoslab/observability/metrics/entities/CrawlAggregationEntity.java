package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.observability.Printable;

///..
import java.util.Set;
import java.util.TreeSet;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class CrawlAggregationEntity implements Printable{

    ///
    private final String path;
    private final boolean isUnknown;
    private final Set<String> statuses;

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
    public void appendBytes(final FastAsciiJoiner joiner) {

        joiner.add(this.path);
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Boolean.toString(this.isUnknown));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.lastCalled));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Integer.toString(this.numberOfCalls));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);

        for(final String status : this.statuses) {

            joiner.add(status);
            joiner.add(ApplicationProperties.ARRAY_SEPARATOR_STRING);
        }
    }

    ///
}
