package io.github.clamentos.gattoslab.server.components.connection;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.components.HttpProxy;
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
    private final Map<String, AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>>> httpProxyConnectionPools;
    private final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> tcpProxyConnectionPool;

    ///..
    private final AtomicBoolean isClosed;
    private final AtomicReference<Thread> sweeperReference;

    ///..
    private final int socketSweeperPollPeriod;
    private final int httpServerConnectionAge;

    ///
    public ConnectionsTracker(final SquashingLogger squashingLogger, final ApplicationProperties applicationProperties) {

        this.logger = new Logger();
        this.squashingLogger = squashingLogger;

        this.connections = new ConcurrentHashMap<>();
        this.socketsPerIp = new ConcurrentHashMap<>();

        this.connectionCounter = new AtomicInteger();
        this.connectionOpenedEventCounter = new FastAtomicCounter();
        this.connectionClosedEventCounter = new FastAtomicCounter();

        this.httpProxyConnectionPools = new ConcurrentHashMap<>();

        for(final HttpProxy httpProxy : applicationProperties.getHttpProxyServers()) {

            final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> pool = new AtomicReferenceArray<>(

                ApplicationProperties.MAX_HTTP_PROXY_CONNECTION_POOL_SIZE
            );

            for(int i = 0; i < ApplicationProperties.MAX_HTTP_PROXY_CONNECTION_POOL_SIZE; i++) {

                pool.set(i, new Pair<>(new ReentrantLock(), null));
            }

            this.httpProxyConnectionPools.put(httpProxy.getDomain(), pool);
        }

        this.tcpProxyConnectionPool = new AtomicReferenceArray<>(ApplicationProperties.MAX_TCP_PROXY_CONNECTION_POOL_SIZE);

        for(int i = 0; i < ApplicationProperties.MAX_TCP_PROXY_CONNECTION_POOL_SIZE; i++) {

            this.tcpProxyConnectionPool.set(i, new Pair<>(new ReentrantLock(), null));
        }

        this.isClosed = new AtomicBoolean();
        this.sweeperReference = new AtomicReference<>(GenericUtils.spawnVirtualThread("gattos-lab-http-sweeper-task", this::sweepTask));

        this.socketSweeperPollPeriod = (int)ApplicationProperties.SOCKET_SWEEPER_POLL_PERIOD.toMillis();
        this.httpServerConnectionAge = (int)ApplicationProperties.HTTP_SERVER_KEEP_ALIVE_DURATION.toMillis() / this.socketSweeperPollPeriod;
    }

    ///
    public Connection createConnection(final Socket client) {

        final int currentConnectionCount = this.connectionCounter.getAndUpdate(currentValue -> Math.min(

            currentValue + 1,
            ApplicationProperties.MAX_CLIENT_SOCKETS
        ));

        if(currentConnectionCount >= ApplicationProperties.MAX_CLIENT_SOCKETS) return null;

        final byte[] rawClientAddress = ((InetSocketAddress)client.getRemoteSocketAddress()).getAddress().getAddress();
        final IpAddress clientAddress = rawClientAddress.length == 4 ? new IpV4(rawClientAddress) : new IpV6(rawClientAddress);

        final int currentIpConnectionCount = this.socketsPerIp.computeIfAbsent(clientAddress, _ -> new AtomicInteger()).getAndUpdate(currentValue -> 

            Math.min(currentValue + 1, ApplicationProperties.MAX_SOCKETS_PER_CLIENT_IP)
        );

        if(currentIpConnectionCount >= ApplicationProperties.MAX_SOCKETS_PER_CLIENT_IP) {

            final AtomicInteger socketsPerIpCounter = this.socketsPerIp.get(clientAddress);

            if(socketsPerIpCounter != null && socketsPerIpCounter.decrementAndGet() == 0) {

                this.socketsPerIp.remove(clientAddress);
            }

            this.closeConnection(client);
            return null;
        }

        final Connection connection = new Connection(client, clientAddress, this.httpServerConnectionAge);

        this.connections.put(Thread.currentThread(), connection);
        this.connectionOpenedEventCounter.increment();

        return connection;
    }

    ///..
    public void removeConnection(final Connection connection) {

        this.closeConnection(connection);

        if(this.connections.remove(Thread.currentThread()) != null) {

            this.connectionCounter.decrementAndGet();
            this.connectionClosedEventCounter.increment();
        }

        final IpAddress clientRemoteAddress = connection.getClientRemoteAddress();
        final AtomicInteger socketsPerIpCounter = this.socketsPerIp.get(clientRemoteAddress);

        if(socketsPerIpCounter != null && socketsPerIpCounter.decrementAndGet() == 0) {

            this.socketsPerIp.remove(clientRemoteAddress);
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
    public ProxyConnection getPooledHttpConnection(final HttpProxy httpProxy) throws IOException {

        return this.getPooledConnection(

            this.httpProxyConnectionPools.get(httpProxy.getDomain()),
            httpProxy.getDestinationAddress(),
            ApplicationProperties.HTTP_PROXY_INPUT_BUFFERS_SIZE,
            ApplicationProperties.HTTP_PROXY_OUTPUT_BUFFERS_SIZE,
            ApplicationProperties.MAX_HTTP_PROXY_RESPONSE_SIZE,
            httpProxy.getKeepAliveDuration() / this.socketSweeperPollPeriod
        );
    }

    ///..
    public ProxyConnection getPooledTcpConnection(final Pair<String, Integer> destinationAddress, final int keepAlive) throws IOException {

        return this.getPooledConnection(

            this.tcpProxyConnectionPool,
            destinationAddress,
            ApplicationProperties.TCP_PROXY_IO_BUFFERS_SIZE,
            ApplicationProperties.TCP_PROXY_IO_BUFFERS_SIZE,
            ApplicationProperties.MAX_TCP_PROXY_RESPONSE_SIZE,
            keepAlive / this.socketSweeperPollPeriod
        );
    }

    ///..
    public Pair<Integer, Integer> getHttpPoolMetrics() {

        int idleCount = 0;
        int busyCount = 0;

        for(final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> pool : this.httpProxyConnectionPools.values()) {

            final Pair<Integer, Integer> metrics = this.getPoolMetrics(pool);

            idleCount += metrics.getA();
            busyCount += metrics.getB();
        }

        return new Pair<>(idleCount, busyCount);
    }

    ///..
    public Pair<Integer, Integer> getTcpPoolMetrics() {

        return this.getPoolMetrics(this.tcpProxyConnectionPool);
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
    private void doClose() {

        this.isClosed.set(true);

        final Thread sweeper = this.sweeperReference.get();
        final String sweeperName = sweeper.getName();

        try {

            this.logger.info("Joining '" + sweeperName + "'");
            sweeper.interrupt();

            if(!sweeper.join(ApplicationProperties.CONNECTIONS_TRACKER_SHUTDOWN_TIMEOUT)) {

                this.logger.warning("Timed-out while joining '" + sweeperName + "'");
            }

            this.logger.info("Waiting for connections to finish...");

            final long waitForMillis = 100;
            long timeBudget = ApplicationProperties.CONNECTIONS_TRACKER_SHUTDOWN_TIMEOUT.toMillis();

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

            this.closeConnection(connection);
        }
    }

    ///..
    private void closeConnection(final Closeable connection) {

        if(connection != null) {

            try {

                connection.close();
            }

            catch(final IOException exc) {

                this.logException(exc);
            }
        }
    }

    ///..
    private ProxyConnection getPooledConnection(

        final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> pool,
        final Pair<String, Integer> destinationAddress,
        final int inputBufferSize,
        final int outputBufferSize,
        final long maxTransfer,
        final int age

    ) throws IOException {

        final int sleepAmount = 100;
        final int length = pool.length();

        int index = (int)Thread.currentThread().threadId() % length;
        long timer = ApplicationProperties.PROXY_CONNECTION_POOL_AQUIRE_TIMEOUT.toMillis();

        while(timer > 0) {

            for(int i = 0; i < length; i++) {

                final ProxyConnection connection = this.createConnectionMaybe(

                    pool,
                    index,
                    destinationAddress,
                    inputBufferSize,
                    outputBufferSize,
                    maxTransfer,
                    age
                );

                if(connection != null) return connection;
            }

            if(!GenericUtils.silentSleep(sleepAmount)) break;
            timer -= sleepAmount;
        }

        throw new IOException("Connection pool timeout");
    }

    ///..
    private ProxyConnection createConnectionMaybe(

        final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> pool,
        final int index,
        final Pair<String, Integer> destination,
        final int inputBufferSize,
        final int outputBufferSize,
        final long maxTransfer,
        final int keepAlive

    ) throws IOException {

        final Pair<ReentrantLock, ProxyConnection> entry = pool.get(index);
        final ProxyConnection connection = entry.getB();

        if(connection == null) {

            final ReentrantLock lock = entry.getA();
            lock.lock();

            if(entry.getB() == null) {

                try {

                    final ProxyConnection newConnection = new ProxyConnection(

                        index,
                        new Socket(destination.getA(), destination.getB()),
                        inputBufferSize,
                        outputBufferSize,
                        maxTransfer,
                        keepAlive
                    );

                    newConnection.aquire();
                    entry.setB(newConnection);
                    lock.unlock();

                    return newConnection;
                }

                catch(final IOException exc) {

                    lock.unlock();
                    throw exc;
                }
            }

            lock.unlock();
            return null;
        }

        if(connection.aquire()) {

            if(connection.getAge() <= 1 || !connection.isValid()) {

                this.closeConnection(connection);

                final ProxyConnection newConnection = new ProxyConnection(

                    index,
                    new Socket(destination.getA(), destination.getB()),
                    inputBufferSize,
                    outputBufferSize,
                    maxTransfer,
                    keepAlive
                );

                newConnection.aquire();
                entry.setB(newConnection);

                return newConnection;
            }

            return connection;
        }

        return null;
    }

    ///..
    private Pair<Integer, Integer> getPoolMetrics(final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> pool) {

        int idleCount = 0;
        int busyCount = 0;

        for(int i = 0; i < ApplicationProperties.MAX_HTTP_PROXY_CONNECTION_POOL_SIZE; i++) {

            final ProxyConnection connection = pool.get(i).getB();

            if(connection != null) {

                if(connection.getIsBusy().get()) busyCount++;
                else idleCount++;
            }
        }

        return new Pair<>(idleCount, busyCount);
    }

    ///..
    private boolean sweep(final Connection connection) {

        if(connection.getAge() == 0) {

            if(connection instanceof ProxyConnection) this.closeConnection(connection);
            else this.removeConnection(connection);

            return true;
        }

        connection.advanceAge();
        return false;
    }

    ///..
    private void sweepPool(final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> pool) {

        final int length = pool.length();

        for(int i = 0; i < length; i++) {

            final Pair<ReentrantLock, ProxyConnection> entry = pool.get(i);
            final ProxyConnection connection = entry.getB();

            if(connection != null && this.sweep(connection)) {

                entry.setB(null);
            }
        }
    }

    ///..
    private void sweepTask() {

        final long sleepAmount = ApplicationProperties.SOCKET_SWEEPER_POLL_PERIOD.toMillis();

        while(!this.isClosed.get()) {

            for(final Connection connection : this.connections.values()) {

                this.sweep(connection);
            }

            this.sweepPool(this.tcpProxyConnectionPool);

            for(final AtomicReferenceArray<Pair<ReentrantLock, ProxyConnection>> pool : this.httpProxyConnectionPools.values()) {

                this.sweepPool(pool);
            }

            GenericUtils.silentSleep(sleepAmount);
        }
    }

    ///
}
