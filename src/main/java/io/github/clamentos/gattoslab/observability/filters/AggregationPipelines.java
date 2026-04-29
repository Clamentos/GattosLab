package io.github.clamentos.gattoslab.observability.filters;

///
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

///..
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

///..
import org.bson.Document;
import org.bson.conversions.Bson;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class AggregationPipelines {

    ///
    private static final String COND = "$cond";
    private static final String AND = "$and";
    private static final String LESS_THAN = "$lt";
    private static final String GREATER_THAN = "$gt";
    private static final String GREATER_THAN_EQUAL = "$gte";
    private static final String COUNT = "count";

    ///..
    private static final Field<Map<String, List<Object>>> perfPipelineKey = new Field<>(

        EntityField.KEY,
        Map.of("$concat", List.of(Map.of("$toString", "$" + EntityField.HTTP_STATUS), ":", "$" + EntityField.PATH))
    );

    ///..
    private static final List<Bson> PERFORMANCE_METRICS = List.of(

        Aggregates.group(

            groupId(EntityField.TIME_SLOT, EntityField.KEY),
            Accumulators.first(EntityField.IS_OTHERS, "$" + EntityField.IS_OTHERS),
            Accumulators.sum(EntityField.RATE, 1),
            Accumulators.sum(EntityField.LATENCIES.get(0), Map.of(COND, List.of(Map.of(LESS_THAN, List.of("$" + EntityField.LATENCY, 2)), 1, 0))),
            Accumulators.sum(EntityField.LATENCIES.get(1), latency(2, 5)),
            Accumulators.sum(EntityField.LATENCIES.get(2), latency(5, 10)),
            Accumulators.sum(EntityField.LATENCIES.get(3), latency(10, 20)),
            Accumulators.sum(EntityField.LATENCIES.get(4), latency(20, 50)),
            Accumulators.sum(EntityField.LATENCIES.get(5), latency(50, 100)),
            Accumulators.sum(EntityField.LATENCIES.get(6), latency(100, 200)),
            Accumulators.sum(EntityField.LATENCIES.get(7), latency(200, 500)),
            Accumulators.sum(EntityField.LATENCIES.get(8), latency(500, 1000)),
            Accumulators.sum(EntityField.LATENCIES.get(9), Map.of(COND, List.of(Map.of(GREATER_THAN, List.of("$" + EntityField.LATENCY, 1000)), 1, 0)))
        ),

        Aggregates.project(

            Projections.fields(

                Projections.excludeId(),
                computedId(EntityField.KEY),
                computedId(EntityField.TIME_SLOT),
                Projections.include(EntityField.IS_OTHERS),
                Projections.include(EntityField.RATE),
                Projections.computed(EntityField.LATENCY_DISTRIBUTION, EntityField.LATENCIES.stream().map(e -> "$" + e).toList())
            )
        )
    );

    ///..
    private static final Bson PATH_INVOCATIONS = Aggregates.group(

        groupId(EntityField.PATH),
        Accumulators.addToSet(EntityField.HTTP_STATUSES, "$" + EntityField.HTTP_STATUS),
        Accumulators.first(EntityField.IS_OTHERS, "$" + EntityField.IS_OTHERS),
        accumulatorsMin(EntityField.FIRST_INVOCATION, EntityField.TIMESTAMP),
        accumulatorsMax(EntityField.LAST_INVOCATION, EntityField.TIMESTAMP),
        Accumulators.sum(COUNT, 1)
    );

    ///..
    private static final Bson USER_AGENTS = Aggregates.group(

        groupId(EntityField.USER_AGENT),
        accumulatorsMin(EntityField.FIRST_INVOCATION, EntityField.TIMESTAMP),
        accumulatorsMax(EntityField.LAST_INVOCATION, EntityField.TIMESTAMP),
        Accumulators.sum(COUNT, 1)
    );

    ///..
    private static final List<Bson> SYSTEM_METRICS = List.of(

        Aggregates.group(

            groupId(EntityField.TIME_SLOT),
            accumulatorsAverage(EntityField.VIRTUAL_THREADS),
            accumulatorsAverage(EntityField.PLATFORM_THREADS),
            accumulatorsAverage(EntityField.CLASSES_LOADED),
            accumulatorsSum(EntityField.FILE_READS),
            accumulatorsSum(EntityField.FILE_WRITES),
            accumulatorsSum(EntityField.SOCKET_READS),
            accumulatorsSum(EntityField.SOCKET_WRITES),
            accumulatorsSum(EntityField.GC_COUNTS),
            accumulatorsSum(EntityField.GC_PAUSE),
            accumulatorsAverage(EntityField.CPU_LOAD_JVM_USER),
            accumulatorsAverage(EntityField.CPU_LOAD_JVM_SYSTEM),
            accumulatorsAverage(EntityField.CPU_LOAD_MACHINE_TOTAL),
            accumulatorsAverage(EntityField.SYSTEM_MEMORY_USED),
            accumulatorsAverage(EntityField.META_SPACE_USED),
            accumulatorsAverage(EntityField.DIRECT_BUFFERS_USED),
            accumulatorsAverage(EntityField.DIRECT_BUFFERS_MEMORY_USED),
            accumulatorsAverage(EntityField.HEAP_USED),
            accumulatorsAverage(EntityField.STORAGE_USED)
        ),

        Aggregates.project(

            Projections.fields(

                Projections.excludeId(),
                computedId(EntityField.TIME_SLOT),
                projectionsComputedCeil(EntityField.VIRTUAL_THREADS),
                projectionsComputedCeil(EntityField.PLATFORM_THREADS),
                projectionsComputedCeil(EntityField.CLASSES_LOADED),
                projectionsComputedCeil(EntityField.FILE_READS),
                projectionsComputedCeil(EntityField.FILE_WRITES),
                projectionsComputedCeil(EntityField.SOCKET_READS),
                projectionsComputedCeil(EntityField.SOCKET_WRITES),
                projectionsComputedCeil(EntityField.GC_COUNTS),
                projectionsComputedCeil(EntityField.GC_PAUSE),
                projectionsComputedCeil(EntityField.CPU_LOAD_JVM_USER),
                projectionsComputedCeil(EntityField.CPU_LOAD_JVM_SYSTEM),
                projectionsComputedCeil(EntityField.CPU_LOAD_MACHINE_TOTAL),
                projectionsComputedCeil(EntityField.SYSTEM_MEMORY_USED),
                projectionsComputedCeil(EntityField.META_SPACE_USED),
                projectionsComputedCeil(EntityField.DIRECT_BUFFERS_USED),
                projectionsComputedCeil(EntityField.DIRECT_BUFFERS_MEMORY_USED),
                projectionsComputedCeil(EntityField.HEAP_USED),
                projectionsComputedCeil(EntityField.STORAGE_USED)
            )
        )
    );

    ///
    public static List<Bson> performanceMetricsPipeline(final RequestMetricsSearchFilter searchFilter) {

        final List<Bson> aggregation = new ArrayList<>();
        aggregation.add(Aggregates.match(searchFilter.toBsonFilter()));

        aggregation.add(Aggregates.addFields(

            perfPipelineKey,
            new Field<>(EntityField.TIME_SLOT, Map.of("$floor", Map.of("$divide", List.of("$" + EntityField.TIMESTAMP, searchFilter.getBucketSize()))))
        ));

        aggregation.addAll(AggregationPipelines.PERFORMANCE_METRICS);
        return aggregation;
    }

    ///..
    public static List<Bson> invocationMetricsPipeline(final SearchFilter searchFilter) {

        return List.of(Aggregates.match(searchFilter.toBsonFilter()), AggregationPipelines.PATH_INVOCATIONS, Aggregates.sort(Sorts.descending(COUNT)));
    }

    ///..
    public static List<Bson> userAgentMetricsPipeline(final SearchFilter searchFilter) {

        return List.of(Aggregates.match(searchFilter.toBsonFilter()), AggregationPipelines.USER_AGENTS, Aggregates.sort(Sorts.descending(COUNT)));
    }

    ///..
    public static List<Bson> systemMetricsPipeline(final AggregatedSearchFilter searchFilter) {

        final List<Bson> aggregation = new ArrayList<>();
        aggregation.add(Aggregates.match(searchFilter.toBsonFilter()));

        aggregation.add(Aggregates.addFields(new Field<>(

            EntityField.TIME_SLOT,
            Map.of("$floor", Map.of("$divide", List.of("$" + EntityField.TIMESTAMP, searchFilter.getBucketSize())))
        )));

        aggregation.addAll(AggregationPipelines.SYSTEM_METRICS);
        return aggregation;
    }

    ///.
    private static Map<String, Object> latency(final int start, final int end) {

        return Map.of(

            COND,
            List.of(Map.of(AND, List.of(Map.of(GREATER_THAN_EQUAL, List.of("$" + EntityField.LATENCY, start)), Map.of(LESS_THAN, List.of("$" + EntityField.LATENCY, end)))), 1, 0)
        );
    }

    ///..
    private static Document groupId(final String... fieldNames) {

        final Document ids = new Document();
        for(final String fieldName : fieldNames) ids.append(fieldName, "$" + fieldName);

        return ids;
    }

    ///..
    private static Bson computedId(final String fieldName) {

        return Projections.computed(fieldName, "$" + EntityField.ID + "." + fieldName);
    }

    ///..
    private static Bson projectionsComputedCeil(final String fieldName) {

        return Projections.computed(fieldName, Map.of("$ceil", "$" + fieldName));
    }

    ///..
    private static BsonField accumulatorsAverage(final String fieldName) {

        return Accumulators.avg(fieldName, "$" + fieldName);
    }

    ///..
    private static BsonField accumulatorsSum(final String fieldName) {

        return Accumulators.sum(fieldName, "$" + fieldName);
    }

    ///..
    private static BsonField accumulatorsMin(final String fieldName, final String source) {

        return Accumulators.min(fieldName, "$" + source);
    }

    ///..
    private static BsonField accumulatorsMax(final String fieldName, final String source) {

        return Accumulators.max(fieldName, "$" + source);
    }

    ///
}
