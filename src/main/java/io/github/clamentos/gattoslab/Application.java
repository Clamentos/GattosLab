package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.RequestExchanger;
import io.github.clamentos.gattoslab.exchange.filters.AuthorizationFilter;
import io.github.clamentos.gattoslab.exchange.filters.BasicFilter;
import io.github.clamentos.gattoslab.exchange.filters.IngressFilter;
import io.github.clamentos.gattoslab.exchange.filters.RateLimitFilter;
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;
import io.github.clamentos.gattoslab.exchange.handling.ResourceMappings;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

///..
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

///
public class Application {

    ///
    private static final Logger logger = new Logger();

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
    private static void start(final String[] args)
    throws CertificateException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {

        final String pid = Long.toString(ProcessHandle.current().pid());

        logger.info("Starting Gatto's Lab with pid '" + pid + "'");
        Files.write(ApplicationProperties.PID_FILE_PATH, pid.getBytes(), StandardOpenOption.CREATE);

        final ShutdownHook shutdownHook = new ShutdownHook();
        final ClassPriorities classPriorities = new ClassPriorities();

        shutdownHook.add(LoggerRoot.getInstance(), classPriorities.getPriority(LoggerRoot.class));
        Runtime.getRuntime().addShutdownHook(GenericUtils.createVirtualThread("gattos-lab-shutdown-hook-task", shutdownHook));

        final ApplicationProperties applicationProperties = resolveProfile(args.length > 0 ? args[0] : null);

        final SSLContext sslContext = createSSLContext(

            applicationProperties.getSslKeyStorePassword(),
            applicationProperties.getSslTrustStorePassword()
        );

        final BatchScheduler batchScheduler = new BatchScheduler();
        shutdownHook.add(batchScheduler, classPriorities.getPriority(BatchScheduler.class));

        final SquashingLogger squashingLogger = new SquashingLogger(batchScheduler);
        shutdownHook.add(squashingLogger, classPriorities.getPriority(SquashingLogger.class));

        final ObservabilityService observabilityService = new ObservabilityService(batchScheduler);
        shutdownHook.add(observabilityService, classPriorities.getPriority(ObservabilityService.class));

        final HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
        shutdownHook.add(httpClient, classPriorities.getPriority(HttpClient.class));

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

        final BasicFilter basicFilter = new BasicFilter(exceptionHandler, ingressFilter, rateLimitFilter, authorizationFilter);

        final RequestExchanger requestExchanger = new RequestExchanger(

            applicationProperties,
            sslContext,
            basicFilter,
            websiteHandler,
            sessionHandler,
            observabilityHandler
        );

        shutdownHook.add(requestExchanger, classPriorities.getPriority(RequestExchanger.class));
    }

    ///..
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

    ///..
    private static  SSLContext createSSLContext(final String keyStorePassword, final String trustStorePassword)
    throws CertificateException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {

        logger.info("Loading SSL certificate start...");

        final char[] rawKeyStorePassword = keyStorePassword.toCharArray();
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        keyManagerFactory.init(loadKeyStore(rawKeyStorePassword), rawKeyStorePassword);
        trustManagerFactory.init(loadTrustStore(trustStorePassword.toCharArray()));

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        logger.info("Loading SSL certificate end");
        return sslContext;
    }

    ///..
    private static KeyStore loadKeyStore(final char[] password)
    throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {

        try(final InputStream keyStoreStream = new FileInputStream("./keystore.p12")) {

            logger.info("Key store file grabbed " + (keyStoreStream != null));

            final KeyStore loadedKeyStore = KeyStore.getInstance("PKCS12");
            loadedKeyStore.load(keyStoreStream, password);

            return loadedKeyStore;
        }
    }

    ///..
    private static KeyStore loadTrustStore(final char[] password)
    throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {

        try(final InputStream trustStoreStream = new FileInputStream("./truststore.p12")) {

            logger.info("Trust store file grabbed " + (trustStoreStream != null));

            final KeyStore loadedTrustStore = KeyStore.getInstance("PKCS12");
            loadedTrustStore.load(trustStoreStream, password);

            return loadedTrustStore;
        }
    }

    ///
}
