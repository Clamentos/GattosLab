package io.github.clamentos.gattoslab.lifecycle;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.DynamicProperties;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.ingress.IngressHandler;
import io.github.clamentos.gattoslab.ingress.RequestDispatcher;
import io.github.clamentos.gattoslab.ingress.filters.BlacklistFilter;
import io.github.clamentos.gattoslab.observability.ObservabilityController;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.LogsService;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogContainer;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.session.SessionController;
import io.github.clamentos.gattoslab.session.SessionService;
import io.github.clamentos.gattoslab.website.Website;
import io.github.clamentos.gattoslab.website.WebsiteController;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///..
import tools.jackson.databind.json.JsonMapper;

///
@AllArgsConstructor
@Getter

///
public final class BeansContainer {

    ///
    private final ApplicationProperties applicationProperties;
    private final DynamicProperties dynamicProperties;
    private final BatchScheduler batchScheduler;
    private final SessionService sessionService;
    private final SessionController sessionController;
    private final MongoClientWrapper mongoClientWrapper;
    private final SquashedLogContainer squashedLogContainer;
    private final JsonMapper jsonMapper;
    private final LogsService logsService;
    private final Website website;
    private final WebsiteController websiteController;
    private final ObservabilityService observabilityService;
    private final ObservabilityController observabilityController;
    private final RequestDispatcher requestDispatcher;
    private final GlobalExceptionHandler globalExceptionHandler;
    private final BlacklistFilter blacklistFilter;
    private final IngressHandler ingressHandler;

    ///..
}
