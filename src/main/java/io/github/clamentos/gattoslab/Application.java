package io.github.clamentos.gattoslab;

///
import com.fasterxml.jackson.annotation.JsonInclude;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.ProfileResolver;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicProperties;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.ingress.IngressHandler;
import io.github.clamentos.gattoslab.ingress.RequestDispatcher;
import io.github.clamentos.gattoslab.ingress.filters.BlacklistFilter;
import io.github.clamentos.gattoslab.lifecycle.ShutdownHook;
import io.github.clamentos.gattoslab.observability.ObservabilityController;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.LogsService;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogsContainer;
import io.github.clamentos.gattoslab.observability.logging.squash.BlacklistSquash;
import io.github.clamentos.gattoslab.observability.logging.squash.IfModifiedSinceMalformedSquash;
import io.github.clamentos.gattoslab.observability.logging.squash.RateLimitSquash;
import io.github.clamentos.gattoslab.persistence.MongoClientProvider;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.session.SessionController;
import io.github.clamentos.gattoslab.session.SessionService;
import io.github.clamentos.gattoslab.utils.ThreadSpawner;
import io.github.clamentos.gattoslab.utils.VirtualThreadExecutor;
import io.github.clamentos.gattoslab.website.Website;
import io.github.clamentos.gattoslab.website.WebsiteController;

///..
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.Undertow.Builder;
import io.undertow.servlet.Servlets;

///..
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

///..
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

///..
import lombok.extern.slf4j.Slf4j;

///..
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

///
@Slf4j

///
public class Application {

    ///
    public static void main(final String[] args) throws Exception {

        prepareOutputFiles();

        final ApplicationProperties applicationProperties = new ProfileResolver(args.length > 0 ? args[0] : null).getApplicationProperties();
        final List<Object> beansContainer = prepareBeans(applicationProperties);
        final Undertow server = prepareWebserver(applicationProperties, (IngressHandler) beansContainer.getLast());

        log.info("Starting webserver...");
        server.start();
        log.info("Webserver started");

        beansContainer.set(beansContainer.size() - 1, server);
        prepareShutdownHook(beansContainer);
    }

    ///.
    private static void prepareOutputFiles() throws IOException {

        final long pid = ProcessHandle.current().pid();
        final PrintStream consoleOut = new PrintStream("./console_out.log");

        System.setOut(consoleOut);
        System.setErr(consoleOut);

        try(final FileWriter pidFile = new FileWriter("./pid.txt")) {

            pidFile.write(Long.toString(pid));
        }

        log.info("Application PID: {}", pid);
    }

    ///..
    public static List<Object> prepareBeans(final ApplicationProperties applicationProperties) throws IOException {

        final BatchScheduler batchScheduler = new BatchScheduler(applicationProperties);
        final SessionService sessionService = new SessionService(applicationProperties, batchScheduler);
        final SessionController sessionController = new SessionController(applicationProperties, sessionService);
        final MongoClientWrapper mongoClientWrapper = new MongoClientWrapper(applicationProperties);

        MongoClientProvider.setWrapper(mongoClientWrapper);

        @SuppressWarnings("squid:S2095") // Closed by shutdown hook
        final SquashedLogsContainer squashedLogsContainer = new SquashedLogsContainer(

            applicationProperties,
            batchScheduler,
            List.of(new IfModifiedSinceMalformedSquash(), new RateLimitSquash(), new BlacklistSquash())
        );

        final JsonMapper jsonMapper = JsonMapper.builder()

            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL).withContentInclusion(JsonInclude.Include.NON_NULL))
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
        ;

        final DynamicProperties dynamicProperties = new DynamicProperties(applicationProperties, batchScheduler, mongoClientWrapper);
        final BlacklistFilter blacklistFilter = new BlacklistFilter(dynamicProperties, squashedLogsContainer);
        final LogsService logsService = new LogsService(applicationProperties, batchScheduler, mongoClientWrapper);
        final Website website = new Website(applicationProperties);
        final WebsiteController websiteController = new WebsiteController(website, squashedLogsContainer);
        final ObservabilityService observabilityService = new ObservabilityService(applicationProperties, batchScheduler, website, mongoClientWrapper);
        final ObservabilityController observabilityController = new ObservabilityController(observabilityService, sessionService, logsService, jsonMapper);
        final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler(applicationProperties, jsonMapper, observabilityService);

        final RequestDispatcher requestDispatcher = new RequestDispatcher(

            jsonMapper,
            globalExceptionHandler,
            website,
            sessionController,
            websiteController,
            observabilityController,
            observabilityService
        );

        final IngressHandler ingressHandler = new IngressHandler(

            applicationProperties,
            blacklistFilter,
            batchScheduler,
            squashedLogsContainer,
            sessionService,
            observabilityService,
            requestDispatcher,
            globalExceptionHandler,
            website
        );

        final List<Object> closableBeans = new ArrayList<>();

        closableBeans.add(observabilityService);
        closableBeans.add(squashedLogsContainer);
        closableBeans.add(batchScheduler);
        closableBeans.add(ingressHandler);

        return closableBeans;
    }

    ///..
    private static Undertow prepareWebserver(final ApplicationProperties applicationProperties, final IngressHandler ingressHandler)
    throws CertificateException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {

        Servlets.deployment()

            .setExecutor(new VirtualThreadExecutor("gattos-lab-ws-worker"))
            .setAsyncExecutor(new VirtualThreadExecutor("gattos-lab-wsa-worker"))
        ;

        final Builder serverBuilder = Undertow.builder().setHandler(ingressHandler);
        final SSLContext sslContext = createSSLContext(applicationProperties.isSslEnabled(), applicationProperties.getSslKeystorePassword());

        serverBuilder.setServerOption(UndertowOptions.MAX_HEADER_SIZE, 8192);
        serverBuilder.setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 4096L);
        serverBuilder.setServerOption(UndertowOptions.MULTIPART_MAX_ENTITY_SIZE, 0L);
        serverBuilder.setServerOption(UndertowOptions.IDLE_TIMEOUT, 30000);
        serverBuilder.setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT, 5000);
        serverBuilder.setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 10000);
        serverBuilder.setServerOption(UndertowOptions.MAX_PARAMETERS, 32);
        serverBuilder.setServerOption(UndertowOptions.MAX_HEADERS, 32);
        serverBuilder.setServerOption(UndertowOptions.MAX_COOKIES, 8);
        serverBuilder.setServerOption(UndertowOptions.MAX_BUFFERED_REQUEST_SIZE, 4096);
        serverBuilder.setServerOption(UndertowOptions.ENABLE_RFC6265_COOKIE_VALIDATION, true);
        serverBuilder.setServerOption(UndertowOptions.MAX_CACHED_HEADER_SIZE, 128);
        serverBuilder.setServerOption(UndertowOptions.HTTP_HEADERS_CACHE_SIZE, 32);
        serverBuilder.setServerOption(UndertowOptions.SHUTDOWN_TIMEOUT, 10000);
        serverBuilder.setServerOption(UndertowOptions.TRACK_ACTIVE_REQUESTS, false);

        if(sslContext != null) {

            serverBuilder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
            serverBuilder.setServerOption(UndertowOptions.HTTP2_SETTINGS_HEADER_TABLE_SIZE, 2048);
            serverBuilder.setServerOption(UndertowOptions.HTTP2_SETTINGS_ENABLE_PUSH, false);
            serverBuilder.setServerOption(UndertowOptions.HTTP2_SETTINGS_MAX_CONCURRENT_STREAMS, 128);
            serverBuilder.setServerOption(UndertowOptions.HTTP2_SETTINGS_INITIAL_WINDOW_SIZE, 65536);
            serverBuilder.setServerOption(UndertowOptions.HTTP2_SETTINGS_MAX_FRAME_SIZE, 16384);
            serverBuilder.setServerOption(UndertowOptions.HTTP2_HUFFMAN_CACHE_SIZE, 256);
            serverBuilder.setServerOption(UndertowOptions.MAX_CONCURRENT_REQUESTS_PER_CONNECTION, 128);
            serverBuilder.setServerOption(UndertowOptions.MAX_QUEUED_READ_BUFFERS, 4);
            serverBuilder.setServerOption(UndertowOptions.RST_FRAMES_TIME_WINDOW, 10000);
            serverBuilder.setServerOption(UndertowOptions.MAX_RST_FRAMES_PER_WINDOW, 128);
            serverBuilder.addHttpsListener(applicationProperties.getServerPort(), applicationProperties.getServerHost(), sslContext);
        }

        else {

            serverBuilder.addHttpListener(applicationProperties.getServerPort(), applicationProperties.getServerHost());
        }

        return serverBuilder.build();
    }

    ///..
    private static void prepareShutdownHook(final List<Object> beansContainer) {

        Runtime.getRuntime().addShutdownHook(ThreadSpawner.createVirtualThread("gattos-lab-shutdown-hook", new ShutdownHook(beansContainer)));
    }

    ///..
    private static SSLContext createSSLContext(final boolean isEnabled, final String password)
    throws CertificateException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {

        if(isEnabled) {

            log.info("Loading SSL certificate start...");
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            keyManagerFactory.init(loadKeyStore("keystore.p12", password), password.toCharArray());

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            log.info("Loading SSL certificate end");
            return sslContext;
        }

        log.info("SSL is not enabled, skipping...");
        return null;
    }

    ///..
    private static KeyStore loadKeyStore(final String name, final String password) throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {

        try(InputStream keyStream = Application.class.getClassLoader().getResourceAsStream(name)) {

            final KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(keyStream, password.toCharArray());

            return loadedKeystore;
        }
    }

    ///
}
