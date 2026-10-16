package io.github.clamentos.gattoslab.http.server;

import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.HashCodedByteArray;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class HttpServerConnections {

    private final Map<Thread, HttpConnection> connections;
    private final Map<HashCodedByteArray, AtomicInteger> socketsPerIp;
    private final AtomicInteger connectionCounter;

    public HttpServerConnections() {

        this.connections = new ConcurrentHashMap<>();
        this.socketsPerIp = new ConcurrentHashMap<>();
        this.connectionCounter = new AtomicInteger();
    }

    public boolean isConnectionsEmpty() {

        return this.connections.isEmpty();
    }

    public Collection<HttpConnection> getConnections() {

        return this.connections.values();
    }

    public Set<Entry<Thread, HttpConnection>> getConnectionEntries() {

        return this.connections.entrySet();
    }

    public void putConnection(final Thread thread, final HttpConnection httpConnection) {

        this.connections.put(thread, httpConnection);
    }

    public void removeConnection(final Thread thread, final HashCodedByteArray address) {

        this.connections.remove(thread);
        this.connectionCounter.decrementAndGet();

        final AtomicInteger socketsPerIpCounter = this.socketsPerIp.get(address);

        if(socketsPerIpCounter != null && socketsPerIpCounter.decrementAndGet() == 0) {

            this.socketsPerIp.remove(address);
        }
    }

    public int updateConnectionCount() {

        return this.connectionCounter.getAndUpdate((currentValue -> Math.min(

            currentValue + 1,
            ApplicationProperties.MAX_SERVER_SOCKETS
        )));
    }

    public int updateIpConnectionCount(final HashCodedByteArray remoteIp) {

        return this.socketsPerIp.computeIfAbsent(remoteIp, _ -> new AtomicInteger()).incrementAndGet();
    }

    public void removeSocketsPerIp(final HashCodedByteArray remoteIp) {

        final AtomicInteger socketsPerIpCounter = this.socketsPerIp.get(remoteIp);

        if(socketsPerIpCounter != null && socketsPerIpCounter.decrementAndGet() == 0) {

            this.socketsPerIp.remove(remoteIp);
        }
    }

    public int getConnectionCount() {

        return this.connectionCounter.get();
    }
}
