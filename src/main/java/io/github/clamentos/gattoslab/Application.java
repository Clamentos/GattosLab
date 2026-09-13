package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.ServerContainer;
import io.github.clamentos.gattoslab.exchange.filters.AuthorizationFilter;
import io.github.clamentos.gattoslab.exchange.filters.IngressFilter;
import io.github.clamentos.gattoslab.exchange.filters.RateLimitFilter;
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;
import io.github.clamentos.gattoslab.exchange.handling.ResourceMappings;
import io.github.clamentos.gattoslab.exchange.handling.RootHandler;
import io.github.clamentos.gattoslab.lifecycle.ClassPriorities;
import io.github.clamentos.gattoslab.lifecycle.ShutdownHook;
import io.github.clamentos.gattoslab.observability.ObservabilityHandler;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.LoggerRoot;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.security.SessionHandler;
import io.github.clamentos.gattoslab.security.SessionService;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.website.WebsiteHandler;

///..
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.List;

///
public class Application {

    ///
    private static final Logger logger = new Logger();
    private static ResourceMappings resourceMappingsForTests = null;

    ///
    public static void main(final String[] args) {

        try {

            start(args);
        }

        catch(final Exception exc) {

            logger.error("Could not start because", exc);
        }
    }

    ///
    public static void start(final String[] args) throws GeneralSecurityException, IOException {

        final String pid = Long.toString(ProcessHandle.current().pid());

        logger.info("Starting Gatto's Lab with pid '" + pid + "'");
        Files.write(ApplicationProperties.PID_FILE_PATH, pid.getBytes(), StandardOpenOption.CREATE);

        final ShutdownHook shutdownHook = new ShutdownHook();
        final ClassPriorities classPriorities = new ClassPriorities();

        shutdownHook.add(LoggerRoot.getInstance(), classPriorities.getPriority(LoggerRoot.class));
        Runtime.getRuntime().addShutdownHook(GenericUtils.createVirtualThread("gattos-lab-shutdown-hook-task", shutdownHook));

        final ApplicationProperties applicationProperties = resolveProfile(args.length > 0 ? args[0] : null);

        final BatchScheduler batchScheduler = new BatchScheduler();
        shutdownHook.add(batchScheduler, classPriorities.getPriority(BatchScheduler.class));

        final SquashingLogger squashingLogger = new SquashingLogger(batchScheduler);
        shutdownHook.add(squashingLogger, classPriorities.getPriority(SquashingLogger.class));

        final ObservabilityService observabilityService = new ObservabilityService(batchScheduler);
        shutdownHook.add(observabilityService, classPriorities.getPriority(ObservabilityService.class));

        final ResourceMappings resourceMappings = new ResourceMappings();
        final SessionService sessionService = new SessionService(applicationProperties, batchScheduler);
        final ExceptionHandler exceptionHandler = new ExceptionHandler(observabilityService);
        final WebsiteHandler websiteHandler = new WebsiteHandler(observabilityService, exceptionHandler);
        final ObservabilityHandler observabilityHandler = new ObservabilityHandler(observabilityService, exceptionHandler);
        final SessionHandler sessionHandler = new SessionHandler(observabilityService, exceptionHandler, sessionService);

        final IngressFilter ingressFilter = new IngressFilter(

            observabilityService,
            applicationProperties,
            squashingLogger,
            resourceMappings
        );

        final RateLimitFilter rateLimitFilter = new RateLimitFilter(observabilityService, squashingLogger, batchScheduler);

        final AuthorizationFilter authorizationFilter = new AuthorizationFilter(

            observabilityService,
            squashingLogger,
            sessionService
        );

        final RootHandler rootHandler = new RootHandler(observabilityService, exceptionHandler, websiteHandler, sessionHandler, observabilityHandler);

        final ServerContainer requestExchanger = new ServerContainer(

            applicationProperties,
            squashingLogger,
            batchScheduler,
            List.of(ingressFilter, rateLimitFilter, authorizationFilter),
            rootHandler
        );

        shutdownHook.add(requestExchanger, classPriorities.getPriority(ServerContainer.class));
        resourceMappingsForTests = resourceMappings;
    }

    ///..
    public static ResourceMappings exposeMappingsForTests() {

        return resourceMappingsForTests;
    }

    ///.
    private static ApplicationProperties resolveProfile(final String profile) throws IOException, IllegalArgumentException {

        if(profile == null || profile.isBlank()) {

            logger.warning("No profile provided, defaulting to 'dev'");
            return new ApplicationProperties("dev");
        }

        else {

            final String profileLower = profile.toLowerCase();

            if(!profileLower.equals("dev") && !profileLower.equals("prod")) {

                logger.warning("Unknown profile '" + profile + "', defaulting to dev");
                return new ApplicationProperties("dev");
            }

            logger.info("Using profile '" + profileLower + "'");
            return new ApplicationProperties(profileLower);
        }
    }

    ///
}
