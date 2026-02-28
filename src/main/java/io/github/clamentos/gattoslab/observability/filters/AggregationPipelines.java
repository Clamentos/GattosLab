package io.github.clamentos.gattoslab.observability.filters;

///
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;

///..
import io.github.clamentos.gattoslab.persistence.EntityField;

import java.util.List;
import java.util.Map;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.bson.Document;
///..
import org.bson.conversions.Bson;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class AggregationPipelines {

    ///
    public static final List<Bson> PERFORMANCE_METRICS = List.of(

        Aggregates.group(

            new Document(Map.of("timeSlot", "$timeSlot", "key", "$key")),
            Accumulators.sum("rate", 1),
            Accumulators.sum("latency_0", Map.of("$cond", List.of(Map.of("$lt", List.of("$latency", 10)), 1, 0))),
            Accumulators.sum("latency_1", Map.of("$cond", List.of(Map.of("$and", List.of(Map.of("$gte", List.of("$latency", 10)), Map.of("$lt", List.of("$latency", 50)))), 1, 0))),
            Accumulators.sum("latency_2", Map.of("$cond", List.of(Map.of("$and", List.of(Map.of("$gte", List.of("$latency", 50)), Map.of("$lt", List.of("$latency", 100)))), 1, 0))),
            // ...
            Accumulators.sum("latency_n", Map.of("$cond", List.of(Map.of("$gte", List.of("$latency", 500)), 1, 0)))
        ),

        Aggregates.project(

            Projections.fields(

                Projections.excludeId(),
                Projections.computed("key", "$_id.key"),
                Projections.computed("timeSlot", "$_id.timeSlot"),
                Projections.include("rate"),
                Projections.computed("latencyDistribution", List.of(

                    "$latency_0",
                    "$latency_1",
                    "$latency_2",
                    "$latency_n"
                ))
            )
        )
    );

    ///..
    public static final Bson PATH_INVOCATIONS = Aggregates.group(

        new Document(EntityField.PATH.getField(), "$" + EntityField.PATH.getField()),
        Accumulators.addToSet("httpStatuses", "$" + EntityField.HTTP_STATUS.getField()),
        Accumulators.min("firstInvocation", "$" + EntityField.TIMESTAMP.getField()),
        Accumulators.max("lastInvocation", "$" + EntityField.TIMESTAMP.getField()),
        Accumulators.sum("count", 1)
    );

    ///..
    public static final Bson USER_AGENTS = Aggregates.group(

        new Document(EntityField.USER_AGENT.getField(), "$" + EntityField.USER_AGENT.getField()),
        Accumulators.min("firstInvocation", "$" + EntityField.TIMESTAMP.getField()),
        Accumulators.max("lastInvocation", "$" + EntityField.TIMESTAMP.getField()),
        Accumulators.sum("count", 1)
    );

    ///
}
