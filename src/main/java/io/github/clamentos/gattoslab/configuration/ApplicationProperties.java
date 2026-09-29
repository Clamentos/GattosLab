package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpHeaderName;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

///..
import lombok.Getter;

///
@Getter

///
public final class ApplicationProperties {

    ///
    public static final byte[] NEW_LINE_BYTES = "\r\n".getBytes();

    ///..
    public static final char FIELD_SEPARATOR = '|';
    public static final char FIELD_SEPARATOR_REPLACEMENT = '\u0001';
    public static final char ARRAY_SEPARATOR = ',';
    public static final char RANGE_SEPARATOR = '-';
    public static final char NEWLINE_REPLACEMENT = '\u0002';
    public static final char LOG_SQUASH_COUNTS_CHAR = '#';
    public static final char ENVIRONMENT_VARIABLE_PREFIX = '$';
    public static final String FIELD_SEPARATOR_URL_ENCODED = "7C";
    public static final String FINGERPRINT_SEPARATOR = " >> ";
    public static final String EXCHANGE_STRING_SEPARATOR = " >> ";

    ///..
    public static final String RATE_LIMIT_REPLENISH_CRON = "s1";
    public static final String SQUASHING_LOGGER_LOG_CRON = "m1";
    public static final String SESSION_SERVICE_MAINTENANCE_CRON = "m1";
    public static final String SYSTEM_METRICS_POLL_CRON = "s5";
    public static final String METRICS_DRAIN_CRON = "s5";
    public static final String OBSERVABILITY_RETENTION_CRON = "m1";
    public static final String SERVER_REGENERATION_CRON = "h24";

    ///..
    public static final Duration SERVER_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration LOG_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration SCHEDULER_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration CORS_DURATION = Duration.ofDays(1);
    public static final Duration CACHE_DURATION = Duration.ofDays(7);
    public static final Duration RATE_LIMIT_BLOCK_DURATION = Duration.ofMinutes(1);
    public static final Duration SESSION_DURATION = Duration.ofHours(2);
    public static final Duration LOGIN_FAILURE_PAUSE_DURATION = Duration.ofMillis(500);
    public static final Duration SERVER_MAX_KEEP_ALIVE_DURATION = Duration.ofMinutes(5);
    public static final Duration LOG_SIPHON_DRAIN_TASK_SLEEP = Duration.ofSeconds(5);
    public static final Duration SCHEDULER_POLL_PERIOD = Duration.ofMillis(100);
    public static final Duration SOCKET_SWEEPER_POLL_PERIOD = Duration.ofMillis(100);
    public static final Duration SOCKET_LINGER = Duration.ofSeconds(1);
    public static final Duration SOCKET_READ_TIMEOUT = Duration.ofSeconds(2);
    public static final String SESSION_COOKIE_NAME = "GattosLabSessionId";
    public static final String UNKNOWN_LOGGER_PLACEHOLDER = "UNKNOWN_LOGGER";
    public static final String UNKNOWN_METHOD_PLACEHOLDER = "UNKNOWN_METHOD";
    public static final String HOMEPAGE_PATH = "/index.html";
    public static final String REDIRECT_PATH = "/login.html";
    public static final String STATIC_SITE_RESOURCES_FOLDER = "site";
    public static final Path LOG_FILE_PATH = Path.of("observability/logs/logs.log");
    public static final Path REQUEST_METRICS_FILE_PATH = Path.of("observability/request/request_metrics.log");
    public static final Path SYSTEM_METRICS_FILE_PATH = Path.of("observability/system/system_metrics.log");
    public static final Path PID_FILE_PATH = Path.of("./pid.txt");
    public static final int MAX_IPS = 1024;
    public static final int SERVER_SOCKET_ACCEPT_QUEUE_SIZE = 1024;
    public static final int MAX_SERVER_SOCKETS = 1024;
    public static final int LOG_SIPHON_CAPACITY = 1024;
    public static final int METRICS_SIPHON_CAPACITY = 4096;
    public static final int MAX_SESSIONS = 4;
    public static final int SESSION_ID_SIZE = 40;
    public static final int MAX_OBSERVABILITY_CHART_LENGTH = 1024;
    public static final int SERVER_INPUT_BUFFERS_SIZE = 4096;
    public static final int SERVER_OUTPUT_BUFFERS_SIZE = 65536;
    public static final int MAX_REQUEST_SIZE = 262_144;
    public static final boolean SOCKET_TCP_NO_DELAY = true;

    ///..
    public static final List<HttpHeader> LOGIN_REDIRECT_HEADERS = List.of(new HttpHeader(HttpHeaderName.LOCATION, REDIRECT_PATH));
    public static final List<HttpHeader> CLEAR_SITE_DATA_HEADERS = List.of(new HttpHeader(HttpHeaderName.CLEAR_SITE_DATA, "cookies"));
    public static final List<HttpHeader> GZIP_HEADER = List.of(new HttpHeader(HttpHeaderName.CONTENT_ENCODING, "gzip"));
    public static final HttpHeader TRANSFER_CHUNKED_HEADER = new HttpHeader(HttpHeaderName.TRANSFER_ENCODING, "chunked");
    public static final HttpHeader CLOSE_CONNECTION_HEADER = new HttpHeader(HttpHeaderName.CONNECTION, "close");
    public static final HttpHeader NO_LENGTH_HEADER = new HttpHeader(HttpHeaderName.CONTENT_LENGTH, "0");

    public static final List<HttpHeader> KEEP_ALIVE_HEADERS = List.of(

        new HttpHeader(HttpHeaderName.CONNECTION, "keep-alive"),
        new HttpHeader(HttpHeaderName.KEEP_ALIVE, "max=" + SERVER_MAX_KEEP_ALIVE_DURATION.toSeconds())
    );

    public static final List<HttpHeader> RETRY_AFTER_HEADERS = List.of(new HttpHeader(

        HttpHeaderName.RETRY_AFTER,
        Long.toString(RATE_LIMIT_BLOCK_DURATION.toSeconds())
    ));

    public static final List<HttpHeader> EXTRA_HEADERS = List.of(

        new HttpHeader(HttpHeaderName.CACHE_CONTROL, "no-cache"),
        new HttpHeader(HttpHeaderName.CONTENT_SECURITY_POLICY, "require-trusted-types-for 'script'"),
        new HttpHeader(HttpHeaderName.X_FRAME_OPTIONS, "SAMEORIGIN"),
        new HttpHeader(HttpHeaderName.STRICT_TRANSPORT_SECURITY, "max-age=63072000; includeSubDomains")
    );

    public static final List<HttpHeader> GZIP_CACHE_HEADERS = List.of(

        new HttpHeader(HttpHeaderName.CONTENT_ENCODING, "gzip"),
        new HttpHeader(HttpHeaderName.CACHE_CONTROL, ("max-age=" + CACHE_DURATION.toSeconds() + ", public"))
    );

    public static final List<HttpHeader> CORS_HEADERS = List.of(

        new HttpHeader(HttpHeaderName.ACCESS_CONTROL_ALLOW_METHODS, GenericUtils.concatenateAsCsv(Arrays.stream(HttpMethod.values()).toList())),
        new HttpHeader(HttpHeaderName.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"),
        new HttpHeader(HttpHeaderName.ACCESS_CONTROL_MAX_AGE, Long.toString(ApplicationProperties.CORS_DURATION.toSeconds()))
    );

    ///.
    private final String serverHost;
    private final int serverPort;
    private final String sslCertificatePath;
    private final String sslKeyStorePassword;
    private final boolean keepAliveByDefault;

    ///..
    private final Set<String> allowedOrigins;

    ///..
    private final String loginPassword;
    private final String cookieProperties;

    ///..
    private final String privilegedStaticResourcePathPrefix;
    private final String diskBasedStaticResourcePathSegment;
    private final Duration observabilityDataRetention;
    private final int rateLimitAmount;
    private final Set<Pair<InetAddress, InetAddress>> blockedIpV4s;
    private final Set<Pair<InetAddress, InetAddress>> blockedIpV6s;
    private final Set<String> illegalUserAgentContains;

    ///
    public ApplicationProperties(final String environment) throws IOException, IllegalArgumentException {

        final Properties properties = new Properties();
        final String fileName = "application-" + String.valueOf(environment).toLowerCase(Locale.US) + ".properties";
        final InputStream propertiesFile = ApplicationProperties.class.getClassLoader().getResourceAsStream(fileName);

        if(propertiesFile == null) throw new IllegalArgumentException("Properties file '" + fileName + "' not found");
        properties.load(propertiesFile);

        this.serverHost = this.resolveProperty(properties, "serverHost", String.class, false);

        this.serverPort = this.resolveProperty(properties, "serverPort", Integer.class, false);
        if(this.serverPort < 0 && this.serverPort > 65535) throw new IllegalArgumentException("'serverPort' must be between 0 and 65535");

        this.sslCertificatePath = this.resolveProperty(properties, "sslCertificatePath", String.class, false);
        this.sslKeyStorePassword = this.resolveProperty(properties, "sslKeyStorePassword", String.class, false);
        this.keepAliveByDefault = this.resolveProperty(properties, "keepAliveByDefault", Boolean.class, false);

        this.allowedOrigins = GenericUtils.fastSplit(this.resolveProperty(properties, "allowedOrigins", String.class, false), ARRAY_SEPARATOR)

            .stream()
            .collect(Collectors.toSet())
        ;

        this.loginPassword = this.resolveProperty(properties, "loginPassword", String.class, false);
        this.cookieProperties = this.resolveProperty(properties, "cookieProperties", String.class, false);
        this.privilegedStaticResourcePathPrefix = this.resolveProperty(properties, "privilegedStaticResourcePathPrefix", String.class, false);
        this.diskBasedStaticResourcePathSegment = this.resolveProperty(properties, "diskBasedStaticResourcePathSegment", String.class, false);

        final int observabilityDataRetentionDays = this.resolveProperty(properties, "observabilityDataRetentionDays", Integer.class, false);
        if(observabilityDataRetentionDays <= 0) throw new IllegalArgumentException("Observability retention days must be greater than 0");
        this.observabilityDataRetention = Duration.ofDays(observabilityDataRetentionDays);

        this.rateLimitAmount = this.resolveProperty(properties, "rateLimitAmount", Integer.class, false);
        if(this.rateLimitAmount <= 0) throw new IllegalArgumentException("Rate limit amount must be greater than 0");

        this.blockedIpV4s = this.parseAddressRanges(this.resolveProperty(properties, "blockedIpV4s", String.class, true));
        this.blockedIpV6s = this.parseAddressRanges(this.resolveProperty(properties, "blockedIpV6s", String.class, true));
        this.illegalUserAgentContains = this.parseList(this.resolveProperty(properties, "illegalUserAgentContains", String.class, true));
    }

    ///
    private <T> T resolveProperty(

        final Properties properties,
        final String propertyName,
        final Class<T> clazz,
        final boolean isEmptyAllowed

    ) throws ClassCastException, IllegalArgumentException {

        String propertyValue = properties.getProperty(propertyName);

        if(propertyValue == null) throw new IllegalArgumentException("The property '" + propertyName + "' must be defined");
        if(!isEmptyAllowed && propertyValue.isEmpty()) throw new IllegalArgumentException("The property '" + propertyName + "' cannot be empty");

        if(propertyValue.isEmpty()) {

            if(clazz == String.class) return clazz.cast("");
            if(clazz == Integer.class) return clazz.cast(0);
            if(clazz == Boolean.class) return clazz.cast(false);
        }

        if(propertyValue.charAt(0) == ENVIRONMENT_VARIABLE_PREFIX) {

            final String envName = propertyValue.substring(1);
            propertyValue = System.getenv(envName);

            if(propertyValue == null) throw new IllegalArgumentException("The property '" + propertyName + "' must be defined");
            if(!isEmptyAllowed && propertyValue.isEmpty()) throw new IllegalArgumentException("The property '" + propertyName + "' cannot be empty");
        }

        if(clazz == String.class) return clazz.cast(propertyValue);
        if(clazz == Integer.class) return clazz.cast(Integer.parseInt(propertyValue));
        if(clazz == Boolean.class) return clazz.cast(Boolean.parseBoolean(propertyValue));

        throw new IllegalArgumentException("Unsupported class '" + clazz + "'");
    }

    ///..
    private Set<Pair<InetAddress, InetAddress>> parseAddressRanges(final String data) throws IllegalArgumentException {

        if(data == null || data.isBlank()) return Set.of();

        final Set<Pair<InetAddress, InetAddress>> addressRanges = new LinkedHashSet<>();
        final List<String> splits = GenericUtils.fastSplit(data, ARRAY_SEPARATOR);
        final int length = splits.size();

        for(int i = 0; i < length; i++) {

            final String split = splits.get(i);
            final List<String> range = GenericUtils.fastSplit(split, RANGE_SEPARATOR);

            if(range.size() != 2) throw new IllegalArgumentException("Address ranges must have 2 components");

            final String start = range.get(0);
            final String end = range.get(1);

            if(start == null) throw new IllegalArgumentException("Start address cannot be null");
            if(end == null) throw new IllegalArgumentException("End address cannot be null");
            if(start.compareTo(end) > 0) throw new IllegalArgumentException("Start address must be smaller than end address");

            addressRanges.add(new Pair<>(InetAddress.ofLiteral(range.get(0).trim()), InetAddress.ofLiteral(range.get(1).trim())));
        }

        return addressRanges;
    }

    ///..
    private Set<String> parseList(final String data) {

        if(data == null || data.isBlank()) return Set.of();
        final List<String> splits = GenericUtils.fastSplit(data, ARRAY_SEPARATOR);

        return splits.stream().map(elem -> {

            final String trimmedElem = elem.trim();

            if("null".equals(trimmedElem)) return null;
            if(trimmedElem.isEmpty() || "\"\"".equals(trimmedElem)) return "";

            return trimmedElem;

        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    ///
}
