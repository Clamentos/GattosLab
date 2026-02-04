package io.github.clamentos.gattoslab.observability.metrics.entries;

///
import org.bson.Document;
import org.bson.types.ObjectId;

///.
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
public final class MetricsEntry extends Document {

    ///
    public void createId() {

        this.put("_id", new ObjectId());
    }

    ///..
    public @NonNull String getPath() {

        return (String)this.get("path");
    }

    ///..
    public void setPath(@NonNull final String path) {

        this.put("path", path);
    }

    ///..
    public @Nullable String getUserAgent() {

        return (String)this.get("userAgent");
    }

    ///..
    public void setUserAgent(@Nullable final String userAgent) {

        this.put("userAgent", userAgent);
    }

    ///..
    public boolean isOthers() {

        return (boolean)this.get("isOthers");
    }

    ///..
    public void setOthers(final boolean isOthers) {

        this.put("isOthers", isOthers);
    }

    ///..
    public long getTimestamp() {

        return (long)this.get("timestamp");
    }

    ///..
    public void setTimestamp(final long timestamp) {

        this.put("timestamp", timestamp);
    }

    ///..
    public int getLatency() {

        return (int)this.get("latency");
    }

    ///..
    public void setLatency(final int latency) {

        this.put("latency", latency);
    }

    ///..
    public short getHttpStatus() {

        return (short)this.get("httpStatus");
    }

    ///..
    public void setHttpStatus(final short httpStatus) {

        this.put("httpStatus", httpStatus);
    }

    ///
}
