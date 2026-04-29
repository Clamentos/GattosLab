package io.github.clamentos.gattoslab.lifecycle;

///
import io.undertow.Undertow;

///..
import java.io.Closeable;
import java.util.List;

///..
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

///
@AllArgsConstructor
@Slf4j

///
public class ShutdownHook implements Runnable {

    ///
    private final List<Object> closables;

    ///
    @Override
    public void run() {

        log.info("Begin shutdown...");
        for(final Object closable : closables) this.tryClose(closable);
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

            log.info("Closed {}", closeable.getClass().getSimpleName());
        }

        catch(final Exception exc) {

            log.error("Could not close {} because", closeable.getClass().getSimpleName(), exc);
        }
    }

    ///
}
