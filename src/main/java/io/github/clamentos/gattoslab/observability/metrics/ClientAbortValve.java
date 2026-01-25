package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///.
import jakarta.servlet.ServletException;

///.
import java.io.IOException;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

///..
import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

///..
import org.jspecify.annotations.NonNull;

///
@Component

///
public class ClientAbortValve extends ValveBase {

    ///
    private final ObservabilityService observabilityService;

    ///
    @Autowired
    public ClientAbortValve(@NonNull final ObservabilityService observabilityService) {

        this.observabilityService = observabilityService;
        super.setAsyncSupported(true);
    }

    ///
    @Override
    public void invoke(@NonNull final Request request, @NonNull final Response response) throws IOException, ServletException {

        try {

            super.getNext().invoke(request, response);
        }

        catch(final Exception exc) {

            if(exc instanceof ClientAbortException || exc.getCause() instanceof ClientAbortException) {

                response.setStatus(499);
                observabilityService.afterCompletion(request, response, null, null);
            }

            else {

                throw exc;
            }
        }
    }

    ///
}
