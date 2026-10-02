package io.github.clamentos.gattoslab.lifecycle;

///
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.LoggerRoot;

///..
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

///
public class ShutdownHook implements Runnable {

    ///
    private final Logger logger;
    private final Map<Integer, List<Object>> closeables;

    ///
    public ShutdownHook() {

        this.logger = new Logger();
        this.closeables = new TreeMap<>();
    }

    ///
    public void add(final Closeable closeable, final int priority) {

        this.closeables.computeIfAbsent(priority, _ -> new ArrayList<>()).add(closeable);
    }

    ///..
    public void add(final AutoCloseable autoCloseable, final int priority) {

        this.closeables.computeIfAbsent(priority, _ -> new ArrayList<>()).add(autoCloseable);
    }

    ///..
    @Override
    public void run() {

        this.logger.info("Begin shutdown...");

        for(final List<Object> closeableList : this.closeables.values()) {

            final int length = closeableList.size();

            for(int i = 0; i < length; i++) {

                final Object closeable = closeableList.get(i);

                if(closeable instanceof LoggerRoot) this.logger.info("End shutdown");
                this.tryClose(closeable);
            }
        }
    }

    ///.
    private void tryClose(final Object closeable) {

        if(closeable == null) {

            this.logger.warning("Closeable was null");
            return;
        }

        final String closeableClassName = closeable.getClass().getSimpleName();

        try {

            if(closeable instanceof final Closeable casted) casted.close();
            else ((AutoCloseable)closeable).close();

            this.logger.info("Closed '" + closeableClassName + "'");
        }

        catch(final Exception exc) {

            this.logger.error("Could not close '" + closeableClassName + "'", exc);
        }
    }

    ///
}
