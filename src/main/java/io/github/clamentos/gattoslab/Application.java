package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.lifecycle.ShutdownHook;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.LoggerRoot;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.security.SessionService;
import io.github.clamentos.gattoslab.server.ServerContainer;
import io.github.clamentos.gattoslab.server.components.connection.ConnectionsTracker;
import io.github.clamentos.gattoslab.server.http.exchange.ExceptionHandler;
import io.github.clamentos.gattoslab.server.http.exchange.Filter;
import io.github.clamentos.gattoslab.server.http.exchange.Handler;
import io.github.clamentos.gattoslab.server.http.exchange.ResourceMappings;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.Locale;

///
public class Application {

    ///
    private static final Logger logger = new Logger();

    ///
    public static void main(final String[] args) {

        try {

            start(args);
        }

        catch(final GeneralSecurityException | IOException | RuntimeException exc) {

            logger.error("Could not start", exc);
        }
    }

    ///
    public static void start(final String[] args) throws GeneralSecurityException, IOException {

        final String pid = Long.toString(ProcessHandle.current().pid());

        logger.info("Starting Gatto's Lab with pid '" + pid + "'");
        Files.write(ApplicationProperties.PID_FILE_PATH, pid.getBytes(), StandardOpenOption.CREATE);

        final ShutdownHook shutdownHook = new ShutdownHook();

        shutdownHook.add(LoggerRoot.getInstance());
        Runtime.getRuntime().addShutdownHook(GenericUtils.createVirtualThread("gattos-lab-shutdown-hook-task", shutdownHook));

        final ApplicationProperties applicationProperties = resolveProfile(args.length > 0 ? args[0] : null);
        logger.info("Illegal user path contains: " + applicationProperties.getIllegalPathContains());

        final BatchScheduler batchScheduler = new BatchScheduler();
        shutdownHook.add(batchScheduler);

        final SquashingLogger squashingLogger = new SquashingLogger(batchScheduler);
        shutdownHook.add(squashingLogger);

        final ConnectionsTracker connectionsTracker = new ConnectionsTracker(squashingLogger);
        shutdownHook.add(connectionsTracker);

        final ObservabilityService observabilityService = new ObservabilityService(applicationProperties, connectionsTracker, batchScheduler);
        shutdownHook.add(observabilityService);

        final ResourceMappings resourceMappings = new ResourceMappings(applicationProperties);
        final SessionService sessionService = new SessionService(squashingLogger, applicationProperties, batchScheduler);
        final ExceptionHandler exceptionHandler = new ExceptionHandler(observabilityService);
        final Handler handler = new Handler(observabilityService, sessionService, exceptionHandler, connectionsTracker);

        final Filter filter = new Filter(

            observabilityService,
            applicationProperties,
            squashingLogger,
            resourceMappings,
            sessionService,
            batchScheduler
        );

        final ServerContainer requestExchanger = new ServerContainer(

            applicationProperties,
            squashingLogger,
            batchScheduler,
            connectionsTracker,
            filter,
            handler
        );

        shutdownHook.add(requestExchanger);
    }

    ///.
    private static ApplicationProperties resolveProfile(final String profile) throws IOException, IllegalArgumentException {

        if(profile == null || profile.isBlank()) {

            logger.warning("No profile provided, defaulting to 'dev'");
            return new ApplicationProperties("dev");
        }

        else {

            final String profileLower = profile.toLowerCase(Locale.US);

            if(!"dev".equals(profileLower) && !"prod".equals(profileLower) && !"test".equals(profileLower)) {

                logger.warning("Unknown profile '" + profile + "', defaulting to dev");
                return new ApplicationProperties("dev");
            }

            logger.info("Using profile '" + profileLower + "'");
            return new ApplicationProperties(profileLower);
        }
    }

    ///
}
