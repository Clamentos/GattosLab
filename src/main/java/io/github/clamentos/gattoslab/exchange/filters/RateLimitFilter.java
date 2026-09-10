package io.github.clamentos.gattoslab.exchange.filters;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.HashCodedByteArray;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.filters.components.RateLimitEntry;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

///
public final class RateLimitFilter extends Responder {

    ///
    private final SquashingLogger squashingLogger;

    ///..
    private final Map<HashCodedByteArray, RateLimitEntry> rateLimitMap;
    private final AtomicBoolean tooManyIps;

    ///..
    private final int entryCounterStart;

    ///
    public RateLimitFilter(

        final ObservabilityService observabilityService,
        final SquashingLogger squashingLogger,
        final BatchScheduler batchScheduler
    ) {

        super(observabilityService);

        this.squashingLogger = squashingLogger;

        this.rateLimitMap = new ConcurrentHashMap<>();
        this.tooManyIps = new AtomicBoolean();

        this.entryCounterStart = (int)(ApplicationProperties.RATE_LIMIT_BLOCK_DURATION.toMillis() / batchScheduler.schedule(

            this::replenishTask,
            "gattos-lab-rate-limit-replenish-task",
            ApplicationProperties.RATE_LIMIT_REPLENISH_CRON
        ));
    }

    ///
    public boolean isOk(final HttpExchange exchange) {

        if(this.tooManyIps.get()) {

            this.respondRateLimited(exchange);
            return false;
        }

        final RateLimitEntry rateLimitEntry = this.rateLimitMap.computeIfAbsent(

            new HashCodedByteArray((byte[])exchange.getAttribute(ApplicationProperties.REQUEST_RAW_ADDRESS_ATTRIBUTE)),
            _ -> new RateLimitEntry(ApplicationProperties.RATE_LIMIT_AMOUNT, this.entryCounterStart)
        );

        if(rateLimitEntry.isRateLimited()) {

            this.respondRateLimited(exchange);
            return false;
        }

        return true;
    }

    ///.
    private void respondRateLimited(final HttpExchange exchange) {

        this.squashingLogger.warning("Rate limited '" + GenericUtils.composeFingerprint(exchange) + "' # times");

        super.respond(exchange, HttpStatus.TOO_MANY_REQUESTS, ApplicationProperties.RETRY_AFTER_HEADERS);
        exchange.close();
    }

    ///..
    private void replenishTask() {

        final Iterator<Entry<HashCodedByteArray, RateLimitEntry>> rateLimitEntries = this.rateLimitMap.entrySet().iterator();

        while(rateLimitEntries.hasNext()) {

            final RateLimitEntry rateLimitEntry = rateLimitEntries.next().getValue();
            if(rateLimitEntry.replenish(ApplicationProperties.RATE_LIMIT_AMOUNT)) rateLimitEntries.remove();
        }

        final int uniqueIps = this.rateLimitMap.size();

        if(uniqueIps >= ApplicationProperties.MAX_IPS) {

            this.squashingLogger.warning("Too many ips limit tripped # times");
            this.tooManyIps.set(true);
        }

        else {

            this.tooManyIps.set(false);
        }
    }

    ///
}
