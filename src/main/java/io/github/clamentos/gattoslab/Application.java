package io.github.clamentos.gattoslab;

///
import com.fasterxml.jackson.annotation.JsonInclude;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.ProfileResolver;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicProperties;
import io.github.clamentos.gattoslab.configuration.environments.Environment;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.ingress.RequestDispatcher;
import io.github.clamentos.gattoslab.ingress.filters.AttachmentFilter;
import io.github.clamentos.gattoslab.ingress.filters.BlacklistFilter;
import io.github.clamentos.gattoslab.ingress.filters.CorsFilter;
import io.github.clamentos.gattoslab.ingress.filters.SecurityFilter;
import io.github.clamentos.gattoslab.ingress.filters.ratelimit.RateLimitFilter;
import io.github.clamentos.gattoslab.lifecycle.DeploymentInstanceHandle;
import io.github.clamentos.gattoslab.lifecycle.ShutdownHook;
import io.github.clamentos.gattoslab.observability.ObservabilityController;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.LogsService;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogsContainer;
import io.github.clamentos.gattoslab.observability.logging.squash.BlacklistSquash;
import io.github.clamentos.gattoslab.observability.logging.squash.IfModifiedSinceMalformedSquash;
import io.github.clamentos.gattoslab.observability.logging.squash.RateLimitSquash;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEvent;
import io.github.clamentos.gattoslab.persistence.FileDatabase;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.session.SessionController;
import io.github.clamentos.gattoslab.session.SessionRole;
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
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;

///..
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;

///..
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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

        final JsonMapper jsonMapper = JsonMapper.builder()

            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL).withContentInclusion(JsonInclude.Include.NON_NULL))
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
        ;

        final BatchScheduler batchScheduler = new BatchScheduler(applicationProperties);
        final SessionService sessionService = new SessionService(applicationProperties, batchScheduler);
        final SessionController sessionController = new SessionController(applicationProperties, sessionService);
        final FileDatabase fileDatabase = new FileDatabase(jsonMapper);

        final List<SquashLogEvent> squashes = List.of(new IfModifiedSinceMalformedSquash(), new RateLimitSquash(), new BlacklistSquash());

        @SuppressWarnings("squid:S2095") // Closed by shutdown hook
        final SquashedLogsContainer squashedLogsContainer = new SquashedLogsContainer(applicationProperties, batchScheduler, squashes);

        final DynamicProperties dynamicProperties = new DynamicProperties(applicationProperties, batchScheduler, fileDatabase);
        final LogsService logsService = new LogsService(fileDatabase);
        final Website website = new Website(applicationProperties);
        final WebsiteController websiteController = new WebsiteController(website, squashedLogsContainer);
        final ObservabilityService observabilityService = new ObservabilityService(applicationProperties, batchScheduler, website, fileDatabase);
        final ObservabilityController observabilityController = new ObservabilityController(observabilityService, sessionService, logsService, jsonMapper);
        final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler(applicationProperties, jsonMapper, squashedLogsContainer, observabilityService);

        final RequestDispatcher requestDispatcher = new RequestDispatcher(

            jsonMapper,
            globalExceptionHandler,
            website,
            sessionController,
            websiteController,
            observabilityController,
            observabilityService
        );

        final AttachmentFilter attachmentFilter = new AttachmentFilter(observabilityService, globalExceptionHandler);
        final BlacklistFilter blacklistFilter = new BlacklistFilter(dynamicProperties, globalExceptionHandler);
        final RateLimitFilter rateLimitFilter = new RateLimitFilter(applicationProperties, batchScheduler, globalExceptionHandler);
        final CorsFilter corsFilter = new CorsFilter(applicationProperties, globalExceptionHandler);
        final SecurityFilter securityFilter = new SecurityFilter(applicationProperties, SessionRole.ADMIN, sessionService, globalExceptionHandler, website);
        final Undertow server = prepareWebserver(applicationProperties, requestDispatcher, attachmentFilter, blacklistFilter, rateLimitFilter, corsFilter, securityFilter);

        log.info("Starting webserver...");
        server.start();
        log.info("Webserver started");

        final ShutdownHook shutdownHook = new ShutdownHook(List.of(observabilityService, squashedLogsContainer, batchScheduler, server));
        Runtime.getRuntime().addShutdownHook(ThreadSpawner.createVirtualThread("gattos-lab-sh", shutdownHook));
    }

    ///.
    private static void prepareOutputFiles() throws IOException {

        final long pid = ProcessHandle.current().pid();

        try(final FileWriter pidFile = new FileWriter("./pid.txt")) {

            pidFile.write(Long.toString(pid));
        }

        log.info("Application PID: {}", pid);
        log.info("Classpath: {}", System.getProperty("java.class.path"));

        final Path dynamicPropertiesPath = Path.of("./observability/dynamic_properties/gattoslab.conf");

        Files.createDirectories(dynamicPropertiesPath.getParent());
        if(Files.notExists(dynamicPropertiesPath)) Files.createFile(dynamicPropertiesPath);
    }

    ///..
    private static Undertow prepareWebserver(

        final ApplicationProperties applicationProperties,
        final RequestDispatcher requestDispatcher,
        final AttachmentFilter attachmentFilter,
        final BlacklistFilter blacklistFilter,
        final RateLimitFilter rateLimitFilter,
        final CorsFilter corsFilter,
        final SecurityFilter securityFilter

    ) throws CertificateException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, ServletException, UnrecoverableKeyException {

        final DeploymentInfo deploymentInfo = Servlets.deployment()

            .setExecutor(new VirtualThreadExecutor("gattos-lab-ws"))
            .setAsyncExecutor(new VirtualThreadExecutor("gattos-lab-wsa"))
            .setContextPath("/*")
            .setClassLoader(Application.class.getClassLoader())
            .setDeploymentName("gattoslab-servlet")
            .addFilter(new FilterInfo("gattoslab-attchment-filter", AttachmentFilter.class, () -> new DeploymentInstanceHandle<>(attachmentFilter)))
            .addFilter(new FilterInfo("gattoslab-blacklist-filter", BlacklistFilter.class, () -> new DeploymentInstanceHandle<>(blacklistFilter)))
            .addFilterUrlMapping("gattoslab-attchment-filter", "/*", DispatcherType.REQUEST)
            .addFilterUrlMapping("gattoslab-blacklist-filter", "/*", DispatcherType.REQUEST)
            .addServlet(new ServletInfo("gattoslab-request-dispatcher", RequestDispatcher.class, () -> new DeploymentInstanceHandle<>(requestDispatcher)).addMapping("/*"))
        ;

        if(applicationProperties.isRateLimitEnabled()) {

            deploymentInfo.addFilter(new FilterInfo("gattoslab-ratelimit-filter", RateLimitFilter.class, () -> new DeploymentInstanceHandle<>(rateLimitFilter)));
            deploymentInfo.addFilterUrlMapping("gattoslab-ratelimit-filter", "/*", DispatcherType.REQUEST);
        }

        if(applicationProperties.isCorsEnabled()) {

            deploymentInfo.addFilter(new FilterInfo("gattoslab-cors-filter", CorsFilter.class, () -> new DeploymentInstanceHandle<>(corsFilter)));
            deploymentInfo.addFilterUrlMapping("gattoslab-cors-filter", "/*", DispatcherType.REQUEST);
        }

        if(applicationProperties.isSessionsEnabled()) {

            deploymentInfo.addFilter(new FilterInfo("gattoslab-security-filter", SecurityFilter.class, () -> new DeploymentInstanceHandle<>(securityFilter)));
            deploymentInfo.addFilterUrlMapping("gattoslab-security-filter", "/*", DispatcherType.REQUEST);
        }

        final DeploymentManager deploymentManager = ServletContainer.Factory.newInstance().addDeployment(deploymentInfo);
        deploymentManager.deploy();

        final Builder serverBuilder = Undertow.builder().setHandler(deploymentManager.start());
        final SSLContext sslContext = createSSLContext(applicationProperties.isSslEnabled(), applicationProperties.getSslKeystorePassword(), applicationProperties);

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
        serverBuilder.setServerOption(UndertowOptions.SHUTDOWN_TIMEOUT, (int)applicationProperties.getServerShutdownTimeout().toMillis());
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
    private static SSLContext createSSLContext(final boolean isEnabled, final String password, final ApplicationProperties applicationProperties)
    throws CertificateException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {

        if(isEnabled) {

            log.info("Loading SSL certificate start...");
            final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            keyManagerFactory.init(loadKeyStore(password, applicationProperties), password.toCharArray());

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            log.info("Loading SSL certificate end");
            return sslContext;
        }

        log.info("SSL is not enabled, skipping...");
        return null;
    }

    ///..
    private static KeyStore loadKeyStore(final String password, final ApplicationProperties applicationProperties)
    throws CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException {

        final Environment currentEnvironment = applicationProperties.getCurrentEnvironment();
        final String name = applicationProperties.getSslKeystoreName();

        final InputStream keyStream = currentEnvironment == Environment.PROD ?

            new FileInputStream("./" + name) :
            Application.class.getClassLoader().getResourceAsStream(name)
        ;

        try(keyStream) {

            log.info("Keystore file grabbed {}", keyStream != null);

            final KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(keyStream, password.toCharArray());

            return loadedKeystore;
        }
    }

    ///
}
