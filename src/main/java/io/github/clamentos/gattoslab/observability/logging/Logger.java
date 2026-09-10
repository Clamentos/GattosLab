package io.github.clamentos.gattoslab.observability.logging;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;

///..
import java.lang.StackWalker.StackFrame;

///
public final class Logger {

    ///
    private final LoggerRoot loggerRoot;

    ///..
    private final String name;

    ///
    public Logger() {

        final Class<?> callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames

            .skip(1)
            .findFirst()
            .map(StackWalker.StackFrame::getDeclaringClass)
            .orElse(null)
        );

        this(callerClass != null ? callerClass.getSimpleName() : ApplicationProperties.UNKNOWN_LOGGER_PLACEHOLDER);
    }

    ///..
    public Logger(final String name) {

        this.loggerRoot = LoggerRoot.getInstance();
        this.name = name + ".";
    }

    ///
    public void info(final String message) {

        this.loggerRoot.info(this.name + this.getCallerMethod(), message);
    }

    ///..
    public void warning(final String message) {

        this.loggerRoot.warn(this.name + this.getCallerMethod(), message, null);
    }

    ///..
    public void warning(final String message, final Throwable exception) {

        this.loggerRoot.warn(this.name + this.getCallerMethod(), message, exception);
    }

    ///..
    public void error(final String message) {

        this.loggerRoot.error(this.name + this.getCallerMethod(), message, null);
    }

    ///..
    public void error(final String message, final Throwable exception) {

        this.loggerRoot.error(this.name + this.getCallerMethod(), message, exception);
    }

    ///.
    private String getCallerMethod() {

        return StackWalker.getInstance().walk(frames -> frames

            .skip(2)
            .findFirst()
            .map(StackFrame::getMethodName)
            .orElse(ApplicationProperties.UNKNOWN_METHOD_PLACEHOLDER)
        );
    }

    ///
}
