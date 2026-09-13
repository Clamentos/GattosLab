package io.github.clamentos.gattoslab.exchange;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.RootHandler;
import io.github.clamentos.gattoslab.http.server.Filter;
import io.github.clamentos.gattoslab.http.server.HttpServer;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

///..
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

///
public final class ServerContainer implements Closeable {

    ///
    private final Logger logger;
    private final SquashingLogger squashingLogger;

    ///..
    private final ApplicationProperties applicationProperties;
    private final List<Filter> filters;
    private final RootHandler rootHandler;

    ///..
    private final AtomicReference<HttpServer> httpServerReference;

    ///
    public ServerContainer(

        final ApplicationProperties applicationProperties,
        final SquashingLogger squashingLogger,
        final BatchScheduler batchScheduler,
        final List<Filter> filters,
        final RootHandler rootHandler

    ) throws GeneralSecurityException, IOException {

        this.logger = new Logger();
        this.squashingLogger = squashingLogger;

        this.applicationProperties = applicationProperties;
        this.filters = filters;
        this.rootHandler = rootHandler;

        this.httpServerReference = new AtomicReference<>();
        this.instantiateServer(applicationProperties, squashingLogger, filters, rootHandler);

        batchScheduler.schedule(

            this::regenerateServerTask,
            "gattoslab-server-regeneration-task",
            ApplicationProperties.SERVER_REGENERATION_CRON
        );
    }

    ///
    @Override
    public void close() throws IOException {

        this.logger.info("Begin shutdown...");
        this.httpServerReference.get().close();
        this.logger.info("End shutdown");
    }

    ///..
    public void regenerateServerTask() {

        try {

            this.logger.info("Begin server regeneration...");

            this.close();
            this.instantiateServer(applicationProperties, squashingLogger, filters, rootHandler);

            this.logger.info("End server regeneration");
        }

        catch(final GeneralSecurityException | IOException  exc) {

            this.logger.error("Could not regenerate server because", exc);
        }
    }

    ///.
    private void instantiateServer(

        final ApplicationProperties applicationProperties,
        final SquashingLogger squashingLogger,
        final List<Filter> filters,
        final RootHandler rootHandler

    ) throws GeneralSecurityException, IOException {

        final SSLContext sslContext = this.createSSLContext(

            applicationProperties.getSslCertificatePath(),
            applicationProperties.getSslKeyStorePassword()
        );

        this.httpServerReference.set(new HttpServer(

            squashingLogger,
            Inet4Address.ofLiteral(applicationProperties.getServerHost()),
            applicationProperties.getServerPort(),
            sslContext,
            filters,
            rootHandler
        ));
    }

    ///..
    private SSLContext createSSLContext(final String sslCertificatePath, final String keyStorePassword)
    throws GeneralSecurityException, IOException  {

        logger.info("Loading SSL certificate start...");

        final char[] rawKeyStorePassword = keyStorePassword.toCharArray();
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        keyManagerFactory.init(loadKeyStore(sslCertificatePath, rawKeyStorePassword), rawKeyStorePassword);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        logger.info("Loading SSL certificate end");
        return sslContext;
    }

    ///..
    private KeyStore loadKeyStore(final String path, final char[] password) throws GeneralSecurityException, IOException {

        try(final InputStream keyStoreStream = new FileInputStream(path)) {

            logger.info("Key store file grabbed " + (keyStoreStream != null));

            final KeyStore loadedKeyStore = KeyStore.getInstance("PKCS12");
            loadedKeyStore.load(keyStoreStream, password);

            return loadedKeyStore;
        }
    }

    ///
}
