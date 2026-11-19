package io.github.clamentos.gattoslab.server.tunnel;

import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.components.RawProxy;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.datastructures.addresses.IpAddress;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.server.Server;
import io.github.clamentos.gattoslab.server.components.Tunnel;
import io.github.clamentos.gattoslab.server.components.connection.Connection;
import io.github.clamentos.gattoslab.server.components.connection.ConnectionsTracker;
import io.github.clamentos.gattoslab.utils.GenericUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;

public final class TunnelServer extends Server {

    private final Tunnel[] tunnels;
    private final AtomicBoolean isClosed;

    public TunnelServer(

        final SquashingLogger squashingLogger,
        final ConnectionsTracker connectionsTracker,
        final ApplicationProperties applicationProperties,
        final SSLContext sslContext

    ) throws IOException {

        super(new Logger(), squashingLogger, connectionsTracker);

        this.tunnels = this.createTunnels(sslContext, applicationProperties.getTcpServers());
        this.isClosed = new AtomicBoolean();
    }

    @Override
    public void close() throws IOException {

        this.logger.info("Begin shutdown...");
        this.isClosed.set(true);

        try {

            this.stopAcceptors(this.tunnels, ApplicationProperties.SERVER_CLOSE_TIMEOUT);
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            this.logger.error("Interrupted while closing, force quitting");
        }

        this.logger.info("End shutdown");
    }

    private void acceptTunnel(final ServerSocket serverSocket, final Pair<String, Integer> destination) {

        while(!this.isClosed.get()) {

            try {

                final Socket client = serverSocket.accept();
                GenericUtils.spawnVirtualThread("gattos-lab-tcp-receiver", () -> this.handleTunnelReceive(client, destination));
            }

            catch(final IOException | RuntimeException exc) {

                this.connectionsTracker.logException(exc);
            }
        }
    }

    // Receive from client, transmit to destination
    private void handleTunnelReceive(final Socket client, final Pair<String, Integer> destination) {

        final Connection connection = this.connectionsTracker.createConnection(client);
        final IpAddress remoteAddress = connection.getClientRemoteAddress();

        final int lingerAmount = (int)ApplicationProperties.SOCKET_LINGER.toSeconds();
        final Socket[] target = new Socket[1];

        try {

            target[0] = new Socket(destination.getA(), destination.getB());
            GenericUtils.spawnVirtualThread("gattos-lab-tcp-transmitter", () -> this.handleTunnelTransmit(connection, target[0]));

            final InputStream inputStream = client.getInputStream();
            final OutputStream outputStream = target[0].getOutputStream();
            final byte[] buffer = new byte[ApplicationProperties.DEFAULT_STREAM_WRITER_BUFFER_SIZE];

            client.setSoLinger(lingerAmount > 0, lingerAmount);
            client.setTcpNoDelay(ApplicationProperties.SOCKET_TCP_NO_DELAY);
            client.setSoTimeout((int)ApplicationProperties.SERVER_MAX_KEEP_ALIVE_DURATION.toMillis());

            while(connection.isValid()) {

                if(inputStream.read(buffer) != -1) outputStream.write(buffer);
                else break;
            }
        }

        catch(final IOException | RuntimeException exc) {

            if(!connection.isSwept()) this.connectionsTracker.logException(exc);
        }

        connection.getSwept().set(true);
        this.connectionsTracker.closeOrLog(client);
        this.connectionsTracker.closeOrLog(target[0]);
        this.connectionsTracker.removeConnection(Thread.currentThread(), remoteAddress);
    }

    // Receive from destination, transmit to client
    private void handleTunnelTransmit(final Connection client, final Socket destination) {

        try {

            final InputStream inputStream = destination.getInputStream();
            final OutputStream outputStream = client.getSocket().getOutputStream();
            final byte[] buffer = new byte[ApplicationProperties.DEFAULT_STREAM_WRITER_BUFFER_SIZE];

            while(client.isValid()) {

                if(inputStream.read(buffer) != -1) outputStream.write(buffer);
                else break;
            }
        }

        catch(final IOException | RuntimeException exc) {

            if(!client.isSwept()) this.connectionsTracker.logException(exc);
        }
    }

    private Tunnel[] createTunnels(final SSLContext sslContext, final Set<RawProxy> proxies) throws IOException {

        final Tunnel[] newTunnels = new Tunnel[proxies.size()];
        int i = 0;

        for(final RawProxy proxy : proxies) {

            final Pair<String, Integer> listeningAddress = proxy.getListeningAddress();
            final Pair<String, Integer> destinationAddress = proxy.getDestinationAddress();

            final String listening = listeningAddress.getA();
            final int listeningPort = listeningAddress.getB();
            final boolean isSsl = listeningPort == 443 || listeningPort == 8443;
            final ServerSocketFactory serverSocketFactory = isSsl ? sslContext.getServerSocketFactory() : ServerSocketFactory.getDefault();

            this.logger.info("SSL for " + listening + ":" + listeningPort + " is " + isSsl);

            final ServerSocket serverSocket = serverSocketFactory.createServerSocket(

                listeningPort,
                ApplicationProperties.SERVER_SOCKET_ACCEPT_QUEUE_SIZE,
                InetAddress.ofLiteral(listening)
            );

            final Thread acceptorThread = GenericUtils.spawnVirtualThread(

                "gattos-lab-tcp-acceptor-task-" + i,
                () -> this.acceptTunnel(serverSocket, destinationAddress)
            );

            newTunnels[i] = new Tunnel(serverSocket, acceptorThread, destinationAddress);
        }

        return newTunnels;
    }
}
