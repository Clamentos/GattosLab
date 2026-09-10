package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.http.HttpHeader;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

///..
import lombok.Getter;

///
@Getter

///
public final class ApplicationProperties {

    ///
    public static final char FIELD_SEPARATOR = '|';
    public static final char FIELD_SEPARATOR_REPLACEMENT = '\u0001';
    public static final char ARRAY_SEPARATOR = ',';
    public static final char RANGE_SEPARATOR = '-';
    public static final char NEWLINE_REPLACEMENT = '\u0002';
    public static final char NON_ASCII_REPLACEMENT = '?';
    public static final char LOG_SQUASH_COUNTS_CHAR = '#';
    public static final String FIELD_SEPARATOR_STRING = Character.toString(FIELD_SEPARATOR);
    public static final String ARRAY_SEPARATOR_STRING = Character.toString(ARRAY_SEPARATOR);
    public static final String CONFIG_FILE_COMMENT_PREFIX = "#";
    public static final String ENVIRONMENT_VARIABLE_PREFIX = "$";
    public static final String FINGERPRINT_SEPARATOR = ":";
    public static final String EXCHANGE_STRING_SEPARATOR = "::";

    ///..
    public static final String REQUEST_START_TIME_ATTRIBUTE = "REQUEST_START_TIME";      // Long
    public static final String REQUEST_REQUEST_ID_ATTRIBUTE = "REQUEST_ID";              // Long
    public static final String REQUEST_USER_AGENT_ATTRIBUTE = "REQUEST_USER_AGENT";      // String
    public static final String REQUEST_RESOURCE_ATTRIBUTE = "REQUEST_RESOURCE";          // Resource
    public static final String REQUEST_RAW_ADDRESS_ATTRIBUTE = "REQUEST_RAW_ADDRESS";    // byte[]
    public static final String REQUEST_HANDLED_ATTRIBUTE = "REQUEST_HANDLED";            // Boolean
    public static final String REQUEST_TRACKED_ATTRIBUTE = "REQUEST_TRACKED";            // Boolean
    public static final String REQUEST_METHOD_ATTRIBUTE = "REQUEST_METHOD";              // HttpMethod

    ///..
    public static final String RATE_LIMIT_REPLENISH_CRON = "s1";
    public static final String SQUASHING_LOGGER_LOG_CRON = "m1";
    public static final String SESSION_SERVICE_MAINTENANCE_CRON = "m1";
    public static final String SYSTEM_METRICS_POLL_CRON = "s5";
    public static final String METRICS_DRAIN_CRON = "s10";
    public static final String OBSERVABILITY_RETENTION_CRON = "m1";
    public static final String BACKEND_STATUS_RESET_CRON = "m1";

    ///..
    public static final Duration CORS_DURATION = Duration.ofDays(7);
    public static final Duration CACHE_DURATION = Duration.ofDays(7);
    public static final Duration RATE_LIMIT_BLOCK_DURATION = Duration.ofMinutes(1);
    public static final Duration SERVER_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration OBSERVABILITY_DATA_RETENTION = Duration.ofDays(30);
    public static final Duration LOG_CLOSE_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration LOG_SIPHON_DRAIN_TASK_SLEEP = Duration.ofSeconds(10);
    public static final Duration SCHEDULER_SHUTDOWN_TIMEOUT = Duration.ofSeconds(5);
    public static final Duration SCHEDULER_POLL_PERIOD = Duration.ofMillis(100);
    public static final Duration SESSION_DURATION = Duration.ofHours(8);
    public static final Duration LOGIN_FAILURE_PAUSE_DURATION = Duration.ofMillis(500);
    public static final Duration HTTP_THREADS_LINGER = Duration.ofSeconds(5);
    public static final String SESSION_COOKIE_NAME = "GattosLabSessionId";
    public static final String REDIRECT_PATH = "/login.html";
    public static final String STATIC_SITE_RESOURCES_FOLDER = "site";
    public static final String PRIVILEGED_STATIC_RESOURCE_PATH_PREFIX = "/admin/";
    public static final String DISK_BASED_STATIC_RESOURCE_PATH_SEGMENT = "/disk/";
    public static final String UNKNOWN_LOGGER_PLACEHOLDER = "UNKNOWN_LOGGER";
    public static final String UNKNOWN_METHOD_PLACEHOLDER = "UNKNOWN_METHOD";
    public static final String LOG_FILE_PATH = "observability/logs/logs.log";
    public static final String REQUEST_METRICS_FILE_PATH = "observability/request/request_metrics.log";
    public static final String SYSTEM_METRICS_FILE_PATH = "observability/system/system_metrics.log";
    public static final Path PID_FILE_PATH = Path.of("./pid.txt");
    public static final int MAX_IPS = 65536;
    public static final int RATE_LIMIT_AMOUNT = 50; // every RATE_LIMIT_REPLENISH_CRON
    public static final int SERVER_NUMBER_OF_HTTP_THREADS = 1;
    public static final int SERVER_REQUEST_QUEUE_SIZE = 4096;
    public static final int LOG_SIPHON_CAPACITY = 1024;
    public static final int METRICS_SIPHON_CAPACITY = 4096;
    public static final int MAX_SESSIONS = 16;
    public static final int SESSION_ID_SIZE = 40;
    public static final int MAX_OBSERVABILITY_CHART_LENGTH = 1024;

    public static final Set<String> EXCLUDED_FORWARDABLE_HEADERS = Set

        .of("connection", "content-length", "expect", "host", "upgrade", "alt-used")
        .stream()
        .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)))
    ;

    public static final Map<String, List<String>> LOGIN_REDIRECT_HEADERS = Map.of(HttpHeader.LOCATION.getName(), List.of(REDIRECT_PATH));
    public static final Map<String, List<String>> CLEAR_SITE_DATA_HEADERS = Map.of(HttpHeader.CLEAR_SITE_DATA.getName(), List.of("cookies"));
    public static final Map<String, List<String>> NO_CACHE_HEADERS = Map.of(HttpHeader.CACHE_CONTROL.getName(), List.of("no-cache"));

    public static final Map<String, List<String>> RETRY_AFTER_HEADERS = Map.of(

        HttpHeader.RETRY_AFTER.getName(),
        List.of(Long.toString(RATE_LIMIT_BLOCK_DURATION.toSeconds()))
    );

    public static final Map<String, List<String>> EXTRA_HEADERS = Map.of(

        HttpHeader.CONTENT_SECURITY_POLICY.getName(), List.of("require-trusted-types-for 'script'"),
        HttpHeader.X_FRAME_OPTIONS.getName(), List.of("SAMEORIGIN"),
        HttpHeader.STRICT_TRANSPORT_SECURITY.getName(), List.of("max-age=31536000; includeSubDomains")
    );

    public static final Map<String, List<String>> GZIP_HEADERS = Map.of(

        HttpHeader.CONTENT_ENCODING.getName(), List.of("gzip")
    );

    public static final Map<String, List<String>> GZIP_CACHE_HEADERS = GenericUtils.mergeMaps(

        GZIP_HEADERS,
        Map.of(HttpHeader.CACHE_CONTROL.getName(), List.of("max-age=" + CACHE_DURATION.toSeconds(), "public"))
    );

    public static final Map<String, List<String>> CORS_HEADERS = GenericUtils.mutableMapOf(

        Map.entry(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS.getName(), Arrays.stream(HttpMethod.values()).map(HttpMethod::toString).toList()),
        Map.entry(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS.getName(), List.of("true")),
        Map.entry(HttpHeader.ACCESS_CONTROL_MAX_AGE.getName(), List.of(Long.toString(ApplicationProperties.CORS_DURATION.toSeconds())))
    );

    ///.
    private final int serverPort;
    private final String sslKeyStorePassword;
    private final String sslTrustStorePassword;

    ///..
    private final Set<String> allowedOrigins;

    ///..
    private final String loginPassword;
    private final String cookieProperties;

    ///..
    private final Set<Entry<InetAddress, InetAddress>> blockedIpV4s;
    private final Set<Entry<InetAddress, InetAddress>> blockedIpV6s;
    private final Set<String> illegalUserAgentContains;

    ///
    public ApplicationProperties(final String environment) throws IOException, IllegalArgumentException {

        final Properties properties = new Properties();
        final String fileName = "application-" + String.valueOf(environment).toLowerCase() + ".properties";
        final InputStream propertiesFile = ApplicationProperties.class.getClassLoader().getResourceAsStream(fileName);

        if(propertiesFile == null) throw new IllegalArgumentException("Properties file '" + fileName + "' not found");
        properties.load(propertiesFile);

        this.serverPort = this.resolveProperty(properties, "serverPort", Integer.class);
        if(this.serverPort < 0 && this.serverPort > 65535) throw new IllegalArgumentException("Server port must be between 0 and 65535");

        this.sslKeyStorePassword = this.resolveProperty(properties, "sslKeyStorePassword", String.class);

        if(this.sslKeyStorePassword == null || this.sslKeyStorePassword.isBlank()) {

            throw new IllegalArgumentException("SSL key store password cannot be null or blank");
        }

        this.sslTrustStorePassword = this.resolveProperty(properties, "sslTrustStorePassword", String.class);

        if(this.sslTrustStorePassword == null || this.sslTrustStorePassword.isBlank()) {

            throw new IllegalArgumentException("SSL strust store password cannot be null or blank");
        }

        this.allowedOrigins = GenericUtils.fastSplit(this.resolveProperty(properties, "allowedOrigins", String.class), ARRAY_SEPARATOR)

            .stream()
            .collect(Collectors.toSet())
        ;

        if(this.allowedOrigins == null || this.allowedOrigins.isEmpty()) {

            throw new IllegalArgumentException("Allowed origins cannot be null or empty");
        }

        this.loginPassword = this.resolveProperty(properties, "loginPassword", String.class);
        if(this.loginPassword == null || this.loginPassword.isBlank()) throw new IllegalArgumentException("Login password cannot be null or blank");

        this.cookieProperties = this.resolveProperty(properties, "cookieProperties", String.class);
        if(this.cookieProperties == null) throw new IllegalArgumentException("Cookie properties cannot be null");

        this.blockedIpV4s = this.parseAddressRanges(this.resolveProperty(properties, "blockedIpV4s", String.class));
        this.blockedIpV6s = this.parseAddressRanges(this.resolveProperty(properties, "blockedIpV6s", String.class));
        this.illegalUserAgentContains = this.parseList(this.resolveProperty(properties, "illegalUserAgentContains", String.class));

        System.getProperties().put("sun.net.httpserver.idleInterval", "60");
        System.getProperties().put("sun.net.httpserver.maxIdleConnections", "50");
        System.getProperties().put("sun.net.httpserver.drainAmount", "65536");
        System.getProperties().put("sun.net.httpserver.maxReqHeaders", "50");
        System.getProperties().put("sun.net.httpserver.maxReqHeaderSize", "8192");
        System.getProperties().put("sun.net.httpserver.nodelay", "true");

        System.getProperties().put("jdk.httpclient.bufsize", "8192");
        System.getProperties().put("jdk.httpclient.connectionPoolSize", "4");
        System.getProperties().put("jdk.httpclient.redirects.retrylimit", "2");
        System.getProperties().put("jdk.http.maxHeaderSize", "8192");
        System.getProperties().put("jdk.httpclient.keepalive.timeout", Long.toString(Duration.ofMinutes(1).toSeconds()));
    }

    ///
    private <T> T resolveProperty(final Properties properties, final String propertyName, final Class<T> clazz) throws IllegalArgumentException {

        String propertyValue = properties.getProperty(propertyName);
        if(propertyValue == null) throw new IllegalArgumentException("The property '" + propertyName + "' is not defined");

        if(propertyValue.isEmpty()) {

            if(clazz == String.class) return clazz.cast("");
            else if(clazz == Integer.class) throw new IllegalArgumentException("The property '" + propertyName + "' is not defined");
        }

        if(propertyValue.startsWith(ENVIRONMENT_VARIABLE_PREFIX)) {

            final String envName = propertyValue.substring(1);

            propertyValue = System.getenv(envName);
            if(propertyValue == null) throw new IllegalArgumentException("The environment variable '" + envName + "' is not defined");
        }

        if(clazz == String.class) return clazz.cast(propertyValue);
        else if(clazz == Integer.class) return clazz.cast(Integer.parseInt(propertyValue));

        throw new IllegalArgumentException("Unsupported class '" + clazz + "'");
    }

    ///..
    private Set<Entry<InetAddress, InetAddress>> parseAddressRanges(final String data) throws IllegalArgumentException {

        if(data == null || data.isBlank()) return Set.of();

        final Set<Entry<InetAddress, InetAddress>> addressRanges = new LinkedHashSet<>();
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

            addressRanges.add(Map.entry(InetAddress.ofLiteral(range.get(0).trim()), InetAddress.ofLiteral(range.get(1).trim())));
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
