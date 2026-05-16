package io.github.clamentos.gattoslab.observability.logging;

///
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.encoder.EncoderBase;

///..
import io.github.clamentos.gattoslab.exceptions.CauseContainer;
import io.github.clamentos.gattoslab.observability.logging.entities.LogEntity;

///
public final class LogsEncoder extends EncoderBase<ILoggingEvent> {

    ///
    private static final byte[] EMPTY_BYTES = new byte[0];

    ///
    @Override
    public byte[] headerBytes() {

        return EMPTY_BYTES;
    }

    ///..
    @Override
    public byte[] encode(final ILoggingEvent logEvent) {

        final StringBuilder sb = new StringBuilder();
        final String message = logEvent.getFormattedMessage();
        final IThrowableProxy throwableProxy = logEvent.getThrowableProxy(); 

        sb.append(logEvent.getTimeStamp()).append(LogEntity.SECTION_SEPARATOR);
        sb.append(this.normalize(logEvent.getLevel())).append(LogEntity.SECTION_SEPARATOR);
        sb.append(this.normalize(logEvent.getThreadName())).append(LogEntity.SECTION_SEPARATOR);
        sb.append(this.normalize(logEvent.getLoggerName())).append(LogEntity.SECTION_SEPARATOR);
        sb.append(this.normalize(message).replace("\n", LogEntity.MESSAGE_LINE_SEPARATOR));

        if(throwableProxy != null) {

            sb.append(LogEntity.SECTION_SEPARATOR).append(throwableProxy.getClassName()).append(LogEntity.SECTION_SEPARATOR);
            sb.append(this.normalize(throwableProxy.getMessage()).replace("\n", LogEntity.MESSAGE_LINE_SEPARATOR));

            final StackTraceElementProxy[] stacktrace = throwableProxy.getStackTraceElementProxyArray();

            if(stacktrace != null) {

                sb.append(LogEntity.SECTION_SEPARATOR).append(this.formatStacktraceForFile(stacktrace)).append(LogEntity.SECTION_SEPARATOR);
                this.formatStacktraceForFile(throwableProxy, sb);
                sb.deleteCharAt(sb.length() - 1);
            }
        }

        sb.append("\n");
        return sb.toString().getBytes();
    }

    ///..
    @Override
    public byte[] footerBytes() {

        return EMPTY_BYTES;
    }

    ///.
    private String normalize(final Object input) {

        if(input != null) {

            final String str = input.toString();
            return str != null ? str : LogEntity.NULL_REPLACEMENT;
        }

        return LogEntity.NULL_REPLACEMENT;
    }

    ///..
    private String formatStacktraceForFile(final StackTraceElementProxy[] stacktrace) {

        final StringBuilder traceString = new StringBuilder();

        for(final StackTraceElementProxy element : stacktrace) {

            traceString

                .append(this.normalize(element).replace("\n", LogEntity.MESSAGE_LINE_SEPARATOR))
                .append(LogEntity.SECTION_SEPARATOR)
            ;
        }

        if(!traceString.isEmpty()) traceString.deleteCharAt(traceString.length() - 1);
        return this.normalize(traceString.toString());
    }

    ///..
    private void formatStacktraceForFile(final IThrowableProxy exception, final StringBuilder traceString) {

        if(exception != null) {

            final String className = exception.getClassName();
            final String msg = exception.getMessage().replace("\n", LogEntity.MESSAGE_LINE_SEPARATOR);
            final IThrowableProxy cause = exception.getCause();

            if(cause != null) {

                final String causeMsg = cause.getMessage().replace("\n", LogEntity.MESSAGE_LINE_SEPARATOR);

                if(cause.getClassName().equals(CauseContainer.class.getName())) traceString.append("$" + className + ": (" + causeMsg + ") ~ " + msg);
                else traceString.append("$" + className + ": " + msg);

                traceString.append(LogEntity.SECTION_SEPARATOR);
                this.formatStacktraceForFile(cause.getCause(), traceString);
            }

            else {

                traceString.append("$" + className + ": " + msg);
                traceString.append(LogEntity.SECTION_SEPARATOR);
            }
        }
    }

    ///
}
