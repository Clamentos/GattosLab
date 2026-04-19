package io.github.clamentos.gattoslab.website;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class Apis {

    ///
    public static final String AUTH_ENDPOINT = "/api/session";
    public static final String REQUEST_METRICS_ENDPOINT = "/admin/api/observability/request-metrics";
    public static final String INVOCATION_METRICS_ENDPOINT = "/admin/api/observability/invocation-metrics";
    public static final String SYSTEM_METRICS_ENDPOINT = "/admin/api/observability/system-metrics";
    public static final String SESSION_METADATA_ENDPOINT = "/admin/api/observability/sessions-metadata";
    public static final String LOGS_ENDPOINT = "/admin/api/observability/logs";
    public static final String FALLBACK_LOGS_ENDPOINT = "/admin/api/observability/fallback-logs";

    ///
}
