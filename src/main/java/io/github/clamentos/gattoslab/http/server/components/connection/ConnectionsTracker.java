package io.github.clamentos.gattoslab.http.server.components.connection;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAtomicCounter;
import io.github.clamentos.gattoslab.datastructures.address.IpAddress;

///..
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///
public final class ConnectionsTracker {

    ///
    private final Map<Thread, HttpConnection> connections;
    private final Map<IpAddress, AtomicInteger> socketsPerIp;

    ///..
    private final AtomicInteger connectionCounter;
    private final FastAtomicCounter connectionOpenedEventCounter;
    private final FastAtomicCounter connectionClosedEventCounter;

    ///
    public ConnectionsTracker() {

        this.connections = new ConcurrentHashMap<>();
        this.socketsPerIp = new ConcurrentHashMap<>();

        this.connectionCounter = new AtomicInteger();
        this.connectionOpenedEventCounter = new FastAtomicCounter();
        this.connectionClosedEventCounter = new FastAtomicCounter();
    }

    ///
    public int getConnectionCount() {

        return this.connectionCounter.get();
    }

    ///..
    public Collection<HttpConnection> getConnections() {

        return this.connections.values();
    }

    ///..
    public void putConnection(final Thread thread, final HttpConnection httpConnection) {

        this.connections.put(thread, httpConnection);
    }

    ///..
    public int updateConnectionCount() {

        return this.connectionCounter.getAndUpdate((currentValue -> Math.min(

            currentValue + 1,
            ApplicationProperties.MAX_SERVER_SOCKETS
        )));
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
    public int updateSocketsPerIpCount(final IpAddress remoteIp) {

        return this.socketsPerIp.computeIfAbsent(remoteIp, _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    public void removeSocketsPerIp(final IpAddress remoteIp) {

        final AtomicInteger socketsPerIpCounter = this.socketsPerIp.get(remoteIp);

        if(socketsPerIpCounter != null && socketsPerIpCounter.decrementAndGet() == 0) {

            this.socketsPerIp.remove(remoteIp);
        }
    }

    ///..
    public void trackConnectionOpened() {

        this.connectionOpenedEventCounter.increment();
    }

    ///..
    public long getConnectionOpenedEventCount() {

        return this.connectionOpenedEventCounter.get();
    }

    ///..
    public void trackConnectionClosed() {

        this.connectionClosedEventCounter.increment();
    }

    ///..
    public long getConnectionClosedEventCount() {

        return this.connectionClosedEventCounter.get();
    }

    ///
}
