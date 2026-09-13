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
    public static final char NON_ASCII_REPLACEMENT = '\u0007';
    public static final char LOG_SQUASH_COUNTS_CHAR = '#';
    public static final char ENVIRONMENT_VARIABLE_PREFIX = '$';
    public static final String FIELD_SEPARATOR_STRING = Character.toString(FIELD_SEPARATOR);
    public static final String ARRAY_SEPARATOR_STRING = Character.toString(ARRAY_SEPARATOR);
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
    public static final Duration SESSION_DURATION = Duration.ofHours(4);
    public static final Duration LOGIN_FAILURE_PAUSE_DURATION = Duration.ofMillis(500);
    public static final Duration OBSERVABILITY_DATA_RETENTION = Duration.ofDays(30);
    public static final Duration SERVER_MAX_KEEP_ALIVE_DURATION = Duration.ofMinutes(5);
    public static final Duration LOG_SIPHON_DRAIN_TASK_SLEEP = Duration.ofSeconds(5);
    public static final Duration SCHEDULER_POLL_PERIOD = Duration.ofMillis(100);
    public static final Duration SERVER_SWEEPER_POLL_PERIOD = Duration.ofSeconds(1);
    public static final String SESSION_COOKIE_NAME = "GattosLabSessionId";
    public static final String UNKNOWN_LOGGER_PLACEHOLDER = "UNKNOWN_LOGGER";
    public static final String UNKNOWN_METHOD_PLACEHOLDER = "UNKNOWN_METHOD";
    public static final String REDIRECT_PATH = "/login.html";
    public static final String STATIC_SITE_RESOURCES_FOLDER = "site";
    public static final String PRIVILEGED_STATIC_RESOURCE_PATH_PREFIX = "/admin/";
    public static final String DISK_BASED_STATIC_RESOURCE_PATH_SEGMENT = "/disk/";
    public static final Path LOG_FILE_PATH = Path.of("observability/logs/logs.log");
    public static final Path REQUEST_METRICS_FILE_PATH = Path.of("observability/request/request_metrics.log");
    public static final Path SYSTEM_METRICS_FILE_PATH = Path.of("observability/system/system_metrics.log");
    public static final Path PID_FILE_PATH = Path.of("./pid.txt");
    public static final int MAX_IPS = 1024;
    public static final int RATE_LIMIT_AMOUNT = 50; // every RATE_LIMIT_REPLENISH_CRON
    public static final int SERVER_SOCKET_ACCEPT_QUEUE_SIZE = 1024;
    public static final int LOG_SIPHON_CAPACITY = 1024;
    public static final int METRICS_SIPHON_CAPACITY = 4096;
    public static final int MAX_SESSIONS = 16;
    public static final int SESSION_ID_SIZE = 40;
    public static final int MAX_OBSERVABILITY_CHART_LENGTH = 1024;
    public static final int SERVER_IO_BUFFERS_SIZE = 4096;
    public static final int MAX_REQUEST_SIZE = 262144;

    ///..
    public static final Map<HttpHeader, byte[]> LOGIN_REDIRECT_HEADERS = GenericUtils.headers(

        Map.entry(HttpHeader.LOCATION, REDIRECT_PATH.getBytes())
    );

    public static final Map<HttpHeader, byte[]> CLEAR_SITE_DATA_HEADERS = GenericUtils.headers(

        Map.entry(HttpHeader.CLEAR_SITE_DATA, "cookies".getBytes())
    );

    public static final Map<HttpHeader, byte[]> NO_CACHE_HEADERS = GenericUtils.headers(

        Map.entry(HttpHeader.CACHE_CONTROL, "no-cache".getBytes())
    );

    public static final Map<HttpHeader, byte[]> TRANSFER_CHUNKED_HEADERS = GenericUtils.headers(

        Map.entry(HttpHeader.TRANSFER_ENCODING, "chunked".getBytes())
    );

    public static final Map<HttpHeader, byte[]> RETRY_AFTER_HEADERS = GenericUtils.headers(

        Map.entry(HttpHeader.RETRY_AFTER, Long.toString(RATE_LIMIT_BLOCK_DURATION.toSeconds()).getBytes())
    );

    public static final Map<HttpHeader, byte[]> EXTRA_HEADERS = GenericUtils.headers(

        Map.entry(HttpHeader.CONTENT_SECURITY_POLICY, "require-trusted-types-for 'script'".getBytes()),
        Map.entry(HttpHeader.X_FRAME_OPTIONS, "SAMEORIGIN".getBytes()),
        Map.entry(HttpHeader.STRICT_TRANSPORT_SECURITY, "max-age=63072000; includeSubDomains".getBytes())
    );

    public static final Map<HttpHeader, byte[]> GZIP_HEADERS = GenericUtils.headers(Map.entry(HttpHeader.CONTENT_ENCODING, "gzip".getBytes()));

    public static final Map<HttpHeader, byte[]> GZIP_CACHE_HEADERS = GenericUtils.headers(

        Map.entry(HttpHeader.CONTENT_ENCODING, "gzip".getBytes()),
        Map.entry(HttpHeader.CACHE_CONTROL, ("max-age=" + CACHE_DURATION.toSeconds() + ", public").getBytes())
    );

    public static final Map<HttpHeader, byte[]> CORS_HEADERS = GenericUtils.headers(

        Map.entry(

            HttpHeader.ACCESS_CONTROL_ALLOW_METHODS,
            GenericUtils.concatenateAsCsv(Arrays.stream(HttpMethod.values()).toList()).getBytes()
        ),

        Map.entry(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true".getBytes()),
        Map.entry(HttpHeader.ACCESS_CONTROL_MAX_AGE, Long.toString(ApplicationProperties.CORS_DURATION.toSeconds()).getBytes())
    );

    ///.
    private final String serverHost;
    private final int serverPort;
    private final String sslCertificatePath;
    private final String sslKeyStorePassword;

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

        this.serverHost = this.resolveProperty(properties, "serverHost", String.class);
        if(this.serverHost == null || this.serverHost.isBlank()) throw new IllegalArgumentException("Server host address cannot be null or blank");

        this.serverPort = this.resolveProperty(properties, "serverPort", Integer.class);
        if(this.serverPort < 0 && this.serverPort > 65535) throw new IllegalArgumentException("Server port must be between 0 and 65535");

        this.sslCertificatePath = this.resolveProperty(properties, "sslCertificatePath", String.class);

        if(this.sslCertificatePath == null || this.sslCertificatePath.isBlank()) {

            throw new IllegalArgumentException("SSL certificate path cannot be null or blank");
        }

        this.sslKeyStorePassword = this.resolveProperty(properties, "sslKeyStorePassword", String.class);

        if(this.sslKeyStorePassword == null || this.sslKeyStorePassword.isBlank()) {

            throw new IllegalArgumentException("SSL key store password cannot be null or blank");
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
    }

    ///
    private <T> T resolveProperty(final Properties properties, final String propertyName, final Class<T> clazz) throws IllegalArgumentException {

        String propertyValue = properties.getProperty(propertyName);
        if(propertyValue == null) throw new IllegalArgumentException("The property '" + propertyName + "' is not defined");

        if(propertyValue.isEmpty()) {

            if(clazz == String.class) return clazz.cast("");
            if(clazz == Integer.class) throw new IllegalArgumentException("The property '" + propertyName + "' is not defined");
        }

        if(propertyValue.charAt(0) == ENVIRONMENT_VARIABLE_PREFIX) {

            final String envName = propertyValue.substring(1);

            propertyValue = System.getenv(envName);
            if(propertyValue == null) throw new IllegalArgumentException("The environment variable '" + envName + "' is not defined");
        }

        if(clazz == String.class) return clazz.cast(propertyValue);
        if(clazz == Integer.class) return clazz.cast(Integer.parseInt(propertyValue));

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
