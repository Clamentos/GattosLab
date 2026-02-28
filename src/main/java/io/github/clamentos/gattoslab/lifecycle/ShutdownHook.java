package io.github.clamentos.gattoslab.lifecycle;

///
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogContainer;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import io.undertow.Undertow;

///..
import java.io.Closeable;

///..
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

///
@AllArgsConstructor
@Slf4j

///
public class ShutdownHook implements Runnable {

    ///
    private final ObservabilityService observabilityService;
    private final SquashedLogContainer squashedLogContainer;
    private final BatchScheduler batchScheduler;
    private final Undertow webserver;

    ///
    @Override
    public void run() {

        log.info("Begin shutdown...");

        this.tryClose(webserver);
        this.tryClose(batchScheduler);
        this.tryClose(observabilityService);
        this.tryClose(squashedLogContainer);

        log.info("End shutdown");
    }

    ///.
    private void tryClose(final Object closeable) {

        try {

            switch(closeable) {

              case final Closeable cl -> cl.close();
              case final Undertow un -> un.stop();
              default -> log.warn("Unknown closable class {}", closeable.getClass().getSimpleName());
            }
        }

        catch(final Exception exc) {

            log.error("Could not close because", exc);
        }
    }

    ///
}
