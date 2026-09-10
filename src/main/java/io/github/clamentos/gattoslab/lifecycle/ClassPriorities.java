package io.github.clamentos.gattoslab.lifecycle;

///
import io.github.clamentos.gattoslab.exchange.RequestExchanger;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.LoggerRoot;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

///
public final class ClassPriorities {

    ///
    private final Map<Class<?>, Integer> priorities;

    ///
    public ClassPriorities() {

        this.priorities = HashMap.newHashMap(6);

        this.priorities.put(BatchScheduler.class, 0);
        this.priorities.put(HttpClient.class, 1);
        this.priorities.put(RequestExchanger.class, 2);
        this.priorities.put(ObservabilityService.class, 3);
        this.priorities.put(SquashingLogger.class, 4);
        this.priorities.put(LoggerRoot.class, 5);
    }

    ///
    public <T> int getPriority(final Class<T> clazz) {

        return this.priorities.get(clazz);
    }

    ///
}
