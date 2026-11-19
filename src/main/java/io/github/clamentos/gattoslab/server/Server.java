package io.github.clamentos.gattoslab.server;

///
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.server.components.Acceptor;
import io.github.clamentos.gattoslab.server.components.connection.ConnectionsTracker;

///..
import java.io.Closeable;
import java.time.Duration;

///
public abstract class Server implements Closeable {

    ///
    protected final Logger logger;
    protected final SquashingLogger squashingLogger;

    ///..
    protected final ConnectionsTracker connectionsTracker;

    ///
    protected Server(final Logger logger, final SquashingLogger squashingLogger, final ConnectionsTracker connectionsTracker) {

        this.logger = logger;
        this.squashingLogger = squashingLogger;

        this.connectionsTracker = connectionsTracker;
    }

    ///
    protected void stopAcceptors(final Acceptor[] acceptors, final Duration timeout) throws InterruptedException {

        for(final Acceptor siteAcceptor : acceptors) {

            final Thread acceptorThread = siteAcceptor.getAcceptorThread();
            final String name = acceptorThread.getName();

            this.logger.info("Joining '" + name + "'");

            acceptorThread.interrupt();
            if(!acceptorThread.join(timeout)) this.logger.warning("Timed-out while joining '" + name + "'");
        }
    }

    ///
}
