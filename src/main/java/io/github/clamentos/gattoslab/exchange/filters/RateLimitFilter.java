package io.github.clamentos.gattoslab.exchange.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.HashCodedByteArray;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.filters.components.RateLimitEntry;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.Filter;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

///
public final class RateLimitFilter extends Responder implements Filter {

    ///
    private static final String BLOCKED_MESSAGE = "Rate limited";
    private static final byte[] BLOCKED_MESSAGE_BYTES = BLOCKED_MESSAGE.getBytes();

    ///.
    private final SquashingLogger squashingLogger;

    ///..
    private final Map<HashCodedByteArray, RateLimitEntry> rateLimitMap;

    ///..
    private final int rateLimitAmount;
    private final int entryCounterStart;

    ///
    public RateLimitFilter(

        final ApplicationProperties applicationProperties,
        final ObservabilityService observabilityService,
        final SquashingLogger squashingLogger,
        final BatchScheduler batchScheduler
    ) {

        super(observabilityService);

        this.squashingLogger = squashingLogger;
        this.rateLimitMap = new ConcurrentHashMap<>();

        this.rateLimitAmount = applicationProperties.getRateLimitAmount();

        this.entryCounterStart = (int)(ApplicationProperties.RATE_LIMIT_BLOCK_DURATION.toMillis() / batchScheduler.schedule(

            this::replenishTask,
            "gattos-lab-rate-limit-replenish-task",
            ApplicationProperties.RATE_LIMIT_REPLENISH_CRON
        ));
    }

    ///
    @Override
    public boolean filter(final HttpExchange exchange) {

        final RateLimitEntry rateLimitEntry = this.rateLimitMap.computeIfAbsent(

            new HashCodedByteArray(exchange.getRemoteIpAddress()),
            _ -> new RateLimitEntry(this.rateLimitAmount, this.entryCounterStart)
        );

        if(rateLimitEntry.isRateLimited()) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash(BLOCKED_MESSAGE, exchange));
            super.respond(exchange, HttpStatus.TOO_MANY_REQUESTS, ApplicationProperties.RETRY_AFTER_HEADERS, MimeType.TEXT, BLOCKED_MESSAGE_BYTES);
            exchange.close();

            return false;
        }

        return true;
    }

    ///.
    private void replenishTask() {

        for(final Entry<HashCodedByteArray, RateLimitEntry> entry : this.rateLimitMap.entrySet()) {

            if(entry.getValue().replenish(this.rateLimitAmount)) this.rateLimitMap.remove(entry.getKey());
        }
    }

    ///
}
