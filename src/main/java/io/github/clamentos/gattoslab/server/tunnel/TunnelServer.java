package io.github.clamentos.gattoslab.server.tunnel;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.components.RawProxy;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.server.Server;
import io.github.clamentos.gattoslab.server.components.Acceptor;
import io.github.clamentos.gattoslab.server.components.connection.Connection;
import io.github.clamentos.gattoslab.server.components.connection.ConnectionsTracker;
import io.github.clamentos.gattoslab.server.components.connection.ProxyConnection;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;

///..
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;

///
public final class TunnelServer extends Server {

    ///
    private final Acceptor[] acceptors;

    ///
    public TunnelServer(

        final SquashingLogger squashingLogger,
        final ConnectionsTracker connectionsTracker,
        final ApplicationProperties applicationProperties,
        final SSLContext sslContext

    ) throws IOException {

        super(new Logger(), squashingLogger, connectionsTracker);
        this.acceptors = this.createAcceptors(sslContext, applicationProperties.getTcpServers());
    }

    ///
    @Override
    protected void doClose() throws IOException {

        try {

            super.stopAcceptors(this.acceptors, ApplicationProperties.ACCEPTORS_SHUTDOWN_TIMEOUT);
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            super.logger.error("Interrupted while closing, force quitting");
        }
    }

    ///.
    private void accept(final ServerSocket serverSocket, final Pair<String, Integer> destination, final int keepAliveDuration) {

        while(!super.isClosed.get()) {

            try {

                final Socket client = serverSocket.accept();
                GenericUtils.spawnVirtualThread("gattos-lab-tcp-receiver", () -> this.handleReceive(client, destination, keepAliveDuration));
            }

            catch(final IOException | RuntimeException exc) {

                super.connectionsTracker.logException(exc);
            }
        }
    }

    ///..
    private void handleReceive(final Socket client, final Pair<String, Integer> destination, final int keepAliveDuration) {

        final Connection connection = super.connectionsTracker.createConnection(client);
        if(connection == null) return;

        final int lingerAmount = (int)ApplicationProperties.SOCKET_LINGER_DURATION.toSeconds();
        final ProxyConnection[] proxyConnection = new ProxyConnection[1];

        Thread tcpTransmitter = null;

        try {

            client.setSoLinger(lingerAmount > 0, lingerAmount);
            client.setTcpNoDelay(ApplicationProperties.SOCKET_TCP_NO_DELAY);
            client.setSoTimeout(keepAliveDuration);

            proxyConnection[0] = super.connectionsTracker.getPooledTcpConnection(destination, keepAliveDuration);

            tcpTransmitter = GenericUtils.spawnVirtualThread(

                "gattos-lab-tcp-transmitter",
                () -> this.handleTunnelTransmit(connection, proxyConnection[0])
            );

            client.getInputStream().transferTo(proxyConnection[0].getWriter());
        }

        catch(final IOException | RuntimeException exc) {

            if(connection.getAge() > 0) this.connectionsTracker.logException(exc);
        }

        super.connectionsTracker.removeConnection(connection);

        try {

            if(tcpTransmitter != null && !tcpTransmitter.join(ApplicationProperties.ACCEPTORS_SHUTDOWN_TIMEOUT)) {

                this.logger.warning("Timed-out while joining '" + tcpTransmitter.getName() + "'");
            }
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            super.logger.error("Interrupted while joining, force quitting");
        }

        proxyConnection[0].release();
    }

    ///..
    private void handleTunnelTransmit(final Connection client, final ProxyConnection proxyConnection) {

        try {

            proxyConnection.getReader().transferTo(client.getSocket().getOutputStream(), Long.MAX_VALUE);
        }

        catch(final IOException | RuntimeException exc) {

            if(client.getAge() > 0) this.connectionsTracker.logException(exc);
        }
    }

    ///..
    private Acceptor[] createAcceptors(final SSLContext sslContext, final Set<RawProxy> proxies) throws IOException {

        final Acceptor[] newTunnels = new Acceptor[proxies.size()];
        int i = 0;

        for(final RawProxy proxy : proxies) {

            final Pair<String, Integer> listeningAddress = proxy.getListeningAddress();
            final Pair<String, Integer> destinationAddress = proxy.getDestinationAddress();

            final String listening = listeningAddress.getA();
            final int listeningPort = listeningAddress.getB();
            final boolean isSsl = listeningPort == 443 || listeningPort == 8443;
            final ServerSocketFactory serverSocketFactory = isSsl ? sslContext.getServerSocketFactory() : ServerSocketFactory.getDefault();

            super.logger.info("SSL for " + listening + ":" + listeningPort + " is " + isSsl);

            final ServerSocket serverSocket = serverSocketFactory.createServerSocket(

                listeningPort,
                ApplicationProperties.SERVER_SOCKET_ACCEPT_QUEUE_SIZE,
                InetAddress.ofLiteral(listening)
            );

            final Thread acceptorThread = GenericUtils.spawnVirtualThread(

                "gattos-lab-tcp-acceptor-task-" + i,
                () -> this.accept(serverSocket, destinationAddress, proxy.getKeepAliveDuration())
            );

            newTunnels[i] = new Acceptor(serverSocket, acceptorThread);
        }

        return newTunnels;
    }

    ///
}
