package io.github.clamentos.gattoslab.exchange;

///
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.filters.BasicFilter;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.observability.ObservabilityHandler;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.security.SessionHandler;
import io.github.clamentos.gattoslab.website.WebsiteHandler;

///..
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

///..
import javax.net.ssl.SSLContext;

///
public final class RequestExchanger implements Closeable {

    ///
    private final Logger logger;

    ///..
    private final HttpsServer httpServer;

    ///
    public RequestExchanger(

        final ApplicationProperties applicationProperties,
        final SSLContext sslContext,
        final BasicFilter basicFilter,
        final WebsiteHandler websiteHandler,
        final SessionHandler sessionHandler,
        final ObservabilityHandler observabilityHandler

    ) throws IOException {

        this.logger = new Logger();
        java.util.logging.Logger.getLogger("com.sun.net.httpserver").setLevel(Level.SEVERE);

        this.httpServer = HttpsServer.create(

            new InetSocketAddress(applicationProperties.getServerPort()),
            0,
            "/",
            websiteHandler,
            basicFilter
        );

        this.httpServer.createContext(Api.LOGIN.getPath(), sessionHandler).getFilters().add(basicFilter);
        this.httpServer.createContext(Api.LOGOUT.getPath(), sessionHandler).getFilters().add(basicFilter);
        this.httpServer.createContext(Api.GET_SESSIONS.getPath(), sessionHandler).getFilters().add(basicFilter);

        this.httpServer.createContext(Api.GET_LOGS.getPath(), observabilityHandler).getFilters().add(basicFilter);
        this.httpServer.createContext(Api.GET_REQUEST_METRICS.getPath(), observabilityHandler).getFilters().add(basicFilter);
        this.httpServer.createContext(Api.GET_SYSTEM_METRICS.getPath(), observabilityHandler).getFilters().add(basicFilter);
        this.httpServer.createContext(Api.GET_CRAWL_METRICS.getPath(), observabilityHandler).getFilters().add(basicFilter);

        final ThreadPoolExecutor executor = new ThreadPoolExecutor(

            ApplicationProperties.SERVER_NUMBER_OF_HTTP_THREADS,
            ApplicationProperties.SERVER_NUMBER_OF_HTTP_THREADS,
            ApplicationProperties.HTTP_THREADS_LINGER.toMillis(),
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(ApplicationProperties.SERVER_REQUEST_QUEUE_SIZE)
        );

        executor.allowCoreThreadTimeOut(true);

        this.httpServer.setExecutor(executor);
        this.httpServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        this.httpServer.start();
    }

    ///
    @Override
    public void close() throws IOException {

        this.logger.info("Begin shutdown...");
        this.httpServer.stop((int)ApplicationProperties.SERVER_CLOSE_TIMEOUT.toSeconds());
        this.logger.info("End shutdown");
    }

    ///
}
