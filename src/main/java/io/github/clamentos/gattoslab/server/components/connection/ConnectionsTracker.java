package io.github.clamentos.gattoslab.server.components.connection;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAtomicCounter;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.datastructures.addresses.IpAddress;
import io.github.clamentos.gattoslab.datastructures.addresses.IpV4;
import io.github.clamentos.gattoslab.datastructures.addresses.IpV6;
import io.github.clamentos.gattoslab.exceptions.MalformedRequestException;
import io.github.clamentos.gattoslab.exceptions.RequestTooBigException;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

///..
import javax.net.ssl.SSLException;

///
public final class ConnectionsTracker implements Closeable {

    ///
    private final Logger logger;
    private final SquashingLogger squashingLogger;

    ///..
    private final Map<Thread, Connection> connections;
    private final Map<IpAddress, AtomicInteger> socketsPerIp;

    ///..
    private final AtomicInteger connectionCounter;
    private final FastAtomicCounter connectionOpenedEventCounter;
    private final FastAtomicCounter connectionClosedEventCounter;

    ///..
    private final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> proxyConnectionPool;

    ///..
    private final AtomicBoolean isClosed;
    private final AtomicReference<Thread> sweeperReference;

    ///
    public ConnectionsTracker(final SquashingLogger squashingLogger) {

        this.logger = new Logger();
        this.squashingLogger = squashingLogger;

        this.connections = new ConcurrentHashMap<>();
        this.socketsPerIp = new ConcurrentHashMap<>();

        this.connectionCounter = new AtomicInteger();
        this.connectionOpenedEventCounter = new FastAtomicCounter();
        this.connectionClosedEventCounter = new FastAtomicCounter();

        this.proxyConnectionPool = new AtomicReferenceArray<>(ApplicationProperties.PROXY_CONNECTION_POOL_SIZE);

        for(int i = 0; i < ApplicationProperties.PROXY_CONNECTION_POOL_SIZE; i++) {

            this.proxyConnectionPool.set(i, new Pair<>(new ReentrantLock(), null));
        }

        this.isClosed = new AtomicBoolean();
        this.sweeperReference = new AtomicReference<>(GenericUtils.spawnVirtualThread("gattos-lab-http-sweeper-task", this::sweepTask));
    }

    ///
    public Connection createConnection(final Socket client) {

        final IpAddress remoteAddress = this.isConnectionAllowed(client);
        if(remoteAddress == null) return null;

        final Connection connection = new Connection(client, remoteAddress);
        this.connections.put(Thread.currentThread(), connection);

        return connection;
    }

    ///..
    public void removeConnection(final Thread thread, final IpAddress address) {

        this.connections.remove(thread);
        this.connectionCounter.decrementAndGet();

        final AtomicInteger socketsPerIpCounter = this.socketsPerIp.get(address);

        if(socketsPerIpCounter != null && socketsPerIpCounter.decrementAndGet() == 0) {

            this.socketsPerIp.remove(address);
        }
    }

    ///..
    public int getConnectionCount() {

        return this.connectionCounter.get();
    }

    ///..
    public long getConnectionOpenedEventCount() {

        return this.connectionOpenedEventCounter.get();
    }

    ///..
    public long getConnectionClosedEventCount() {

        return this.connectionClosedEventCounter.get();
    }

    ///..
    public AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> getProxyConnectionPool() {

        return this.proxyConnectionPool;
    }

    ///..
    public void closeOrLog(final Socket socket) {

        if(socket != null) {

            try {

                socket.close();
                this.connectionClosedEventCounter.increment();
            }

            catch(final IOException exc) {

                this.logException(exc);
            }
        }
    }

    ///..
    public void logException(final Exception exc) {

        if(!(exc instanceof SocketTimeoutException) && !(exc instanceof SSLException)) {

            if(exc instanceof MalformedRequestException || exc instanceof RequestTooBigException) {

                this.squashingLogger.warning(GenericUtils.composeMessageForSquash(exc.toString()));
            }

            else if(GenericUtils.isExceptionNotable(exc)) {

                this.logger.error("Uncaught exception", exc);
            }
        }
    }

    ///..
    public void reset() {

        this.logger.info("Begin reset...");

        this.doClose();
        this.sweeperReference.set(GenericUtils.spawnVirtualThread("gattos-lab-http-sweeper-task", this::sweepTask));

        this.logger.info("End reset");
    }

    ///..
    @Override
    public void close() throws IOException {

        this.logger.info("Begin shutdown...");
        this.doClose();
        this.logger.info("End shutdown");
    }

    ///.
    private IpAddress isConnectionAllowed(final Socket client) {

        final int currentConnectionCount = this.connectionCounter.getAndUpdate(currentValue -> Math.min(

            currentValue + 1,
            ApplicationProperties.MAX_SERVER_SOCKETS
        ));

        final byte[] rawRemoteAddress = ((InetSocketAddress)client.getRemoteSocketAddress()).getAddress().getAddress();
        final IpAddress remoteAddress = rawRemoteAddress.length == 4 ? new IpV4(rawRemoteAddress) : new IpV6(rawRemoteAddress);
        final int currentIpConnectionCount = this.socketsPerIp.computeIfAbsent(remoteAddress, _ -> new AtomicInteger()).incrementAndGet();

        this.connectionOpenedEventCounter.increment();

        final boolean isConnectionCountOver = currentConnectionCount >= ApplicationProperties.MAX_SERVER_SOCKETS;
        final boolean isIpCountOver = currentIpConnectionCount >= ApplicationProperties.MAX_SOCKETS_PER_IP;

        if(isConnectionCountOver || isIpCountOver) {

            final AtomicInteger socketsPerIpCounter = this.socketsPerIp.get(remoteAddress);

            if(socketsPerIpCounter != null && socketsPerIpCounter.decrementAndGet() == 0) {

                this.socketsPerIp.remove(remoteAddress);
            }

            return null;
        }

        return remoteAddress;
    }

    ///..
    private boolean sweep(final Connection connection, final long keepAliveDuration, final long now) {

        if(connection.getCreatedAt() + keepAliveDuration < now) {

            connection.sweep();
            this.closeOrLog(connection.getSocket());

            return true;
        }

        return false;
    }

    ///..
    private void doClose() {

        this.isClosed.set(true);

        final Thread sweeper = this.sweeperReference.get();
        final String sweeperName = sweeper.getName();

        try {

            this.logger.info("Joining '" + sweeperName + "'");
            sweeper.interrupt();

            if(!sweeper.join(ApplicationProperties.CONNECTIONS_TRACKER_CLOSE_TIMEOUT)) {

                this.logger.warning("Timed-out while joining '" + sweeperName + "'");
            }

            this.logger.info("Waiting for connections to finish...");

            final long waitForMillis = 100;
            long timeBudget = ApplicationProperties.CONNECTIONS_TRACKER_CLOSE_TIMEOUT.toMillis();

            while(this.getConnectionCount() > 0 && timeBudget > 0) {

                Thread.sleep(waitForMillis);
                timeBudget -= waitForMillis;
            }
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            this.logger.error("Interrupted while joining, force quitting");
        }

        this.logger.info("Closing connections...");

        for(final Connection connection : this.connections.values()) {

            connection.sweep();
            this.closeOrLog(connection.getSocket());
        }
    }

    ///..
    private void sweepTask() {

        final long keepAliveDuration = ApplicationProperties.SERVER_MAX_KEEP_ALIVE_DURATION.toMillis();
        final long sleepAmount = ApplicationProperties.SOCKET_SWEEPER_POLL_PERIOD.toMillis();

        while(!this.isClosed.get()) {

            final long now = System.currentTimeMillis();

            for(final Connection connection : this.connections.values()) {

                this.sweep(connection, keepAliveDuration, now);
            }

            for(int i = 0; i < ApplicationProperties.PROXY_CONNECTION_POOL_SIZE; i++) {

                final Pair<ReentrantLock, ProxyConnection> entry = this.getProxyConnectionPool().get(i);
                final ProxyConnection connection = entry.getB();

                if(connection != null && this.sweep(connection, connection.getKeepAliveDuration(), now)) {

                    entry.setB(null);
                }
            }

            GenericUtils.silentSleep(sleepAmount);
        }
    }

    ///
}
