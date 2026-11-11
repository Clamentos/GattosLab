package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    public static final char ILLEGAL_CHAR_REPLACEMENT = '\u0000';
    public static final char FIELD_SEPARATOR = '|';
    public static final char ARRAY_SEPARATOR = ',';
    public static final char LOG_SQUASH_COUNTS_CHAR = '#';
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
    public static final String BLOCKLIST_GENERATION_CRON = "h4";

    ///..
    public static final Duration SERVER_CLOSE_TIMEOUT = Duration.ofSeconds(6);
    public static final Duration LOG_CLOSE_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration SCHEDULER_SHUTDOWN_TIMEOUT = Duration.ofSeconds(4);
    public static final Duration CORS_DURATION = Duration.ofDays(1);
    public static final Duration CACHE_DURATION = Duration.ofDays(7);
    public static final Duration RATE_LIMIT_BLOCK_DURATION = Duration.ofMinutes(1);
    public static final Duration SESSION_DURATION = Duration.ofHours(1);
    public static final Duration LOGIN_FAILURE_PAUSE_DURATION = Duration.ofMillis(500);
    public static final Duration SERVER_MAX_KEEP_ALIVE_DURATION = Duration.ofMinutes(5);
    public static final Duration LOG_SIPHON_DRAIN_TASK_POLL_PERIOD = Duration.ofSeconds(5);
    public static final Duration SCHEDULER_POLL_PERIOD = Duration.ofMillis(100);
    public static final Duration SOCKET_SWEEPER_POLL_PERIOD = Duration.ofSeconds(5);
    public static final Duration SOCKET_LINGER = Duration.ofSeconds(1);
    public static final Duration SOCKET_PRELIMINARY_READ_TIMEOUT = Duration.ofSeconds(2);
    public static final String SESSION_COOKIE_NAME = "GattosLabSessionId=";
    public static final String UNKNOWN_LOGGER_PLACEHOLDER = "UNKNOWN_LOGGER";
    public static final String UNKNOWN_METHOD_PLACEHOLDER = "UNKNOWN_METHOD";
    public static final String HOMEPAGE_PATH = "/index.html";
    public static final String REDIRECT_PATH = "/login.html";
    public static final Path LOG_FILE_PATH = Path.of("observability/logs/logs.log");
    public static final Path REQUEST_METRICS_FILE_PATH = Path.of("observability/request/request_metrics.log");
    public static final Path SYSTEM_METRICS_FILE_PATH = Path.of("observability/system/system_metrics.log");
    public static final Path PID_FILE_PATH = Path.of("./pid.txt");
    public static final Path IPV4_BLOCKLIST_PATH = Path.of("observability/blocklist/ipv4s.txt");
    public static final Path IPV6_BLOCKLIST_PATH = Path.of("observability/blocklist/ipv6s.txt");
    public static final int SERVER_SOCKET_ACCEPT_QUEUE_SIZE = 1024;
    public static final int MAX_SERVER_SOCKETS = 8192;
    public static final int MAX_SOCKETS_PER_IP = 128;
    public static final int LOG_SIPHON_CAPACITY = 1024;
    public static final int METRICS_SIPHON_CAPACITY = 4096;
    public static final int MAX_SESSIONS = 4;
    public static final int SESSION_ID_SIZE = 40;
    public static final int MAX_OBSERVABILITY_CHART_LENGTH = 4096;
    public static final int SERVER_INPUT_BUFFERS_SIZE = 4096;
    public static final int SERVER_OUTPUT_BUFFERS_SIZE = 65536;
    public static final int DEFAULT_STREAM_WRITER_BUFFER_SIZE = 8192;
    public static final int MAX_REQUEST_SIZE = 65536;
    public static final boolean SOCKET_TCP_NO_DELAY = true;

    ///..
    public static final Map<HttpHeader, String> LOGIN_REDIRECT_HEADERS = Map.of(HttpHeader.LOCATION, REDIRECT_PATH);
    public static final Map<HttpHeader, String> CLEAR_SITE_DATA_HEADERS = Map.of(HttpHeader.CLEAR_SITE_DATA, "cookies");
    public static final Map<HttpHeader, String> GZIP_HEADER = Map.of(HttpHeader.CONTENT_ENCODING, "gzip");
    public static final Map<HttpHeader, String> TRANSFER_CHUNKED_HEADER = Map.of(HttpHeader.TRANSFER_ENCODING, "chunked");
    public static final Map<HttpHeader, String> CLOSE_CONNECTION_HEADER = Map.of(HttpHeader.CONNECTION, "close");
    public static final Map<HttpHeader, String> NO_LENGTH_HEADER = Map.of(HttpHeader.CONTENT_LENGTH, "0");

    public static final Map<HttpHeader, String> KEEP_ALIVE_HEADERS = Map.of(

        HttpHeader.CONNECTION, "keep-alive",
        HttpHeader.KEEP_ALIVE, "timeout=" + SERVER_MAX_KEEP_ALIVE_DURATION.toSeconds()
    );

    public static final Map<HttpHeader, String> RETRY_AFTER_HEADERS = Map.of(

        HttpHeader.RETRY_AFTER,
        Long.toString(RATE_LIMIT_BLOCK_DURATION.toSeconds())
    );

    public static final Map<HttpHeader, String> EXTRA_HEADERS = Map.of(

        HttpHeader.CACHE_CONTROL, "no-cache",
        HttpHeader.CONTENT_SECURITY_POLICY, "require-trusted-types-for 'script'",
        HttpHeader.X_FRAME_OPTIONS, "SAMEORIGIN",
        HttpHeader.STRICT_TRANSPORT_SECURITY, "max-age=63072000; includeSubDomains"
    );

    public static final Map<HttpHeader, String> GZIP_CACHE_HEADERS = Map.of(

        HttpHeader.CONTENT_ENCODING, "gzip",
        HttpHeader.CACHE_CONTROL, ("max-age=" + CACHE_DURATION.toSeconds() + ", public")
    );

    public static final Map<HttpHeader, String> CORS_HEADERS = Map.of(

        HttpHeader.ACCESS_CONTROL_ALLOW_METHODS, GenericUtils.concatenateAsCsv(Arrays.stream(HttpMethod.values()).toList()),
        HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true",
        HttpHeader.ACCESS_CONTROL_MAX_AGE, Long.toString(ApplicationProperties.CORS_DURATION.toSeconds())
    );

    ///.
    private final Path sslCertificate;
    private final String sslKeyStorePassword;

    ///..
    private final InetSocketAddress[] siteServers;
    private final Path siteResources;
    private final Set<String> siteAllowedOrigins;
    private final String siteDiskBasedResourcesPathPrefix;

    ///..
    private final InetSocketAddress[] dashboardServers;
    private final Path dashboardResources;
    private final Set<String> dashboardAllowedOrigins;
    private final String loginPassword;
    private final String cookieProperties;
    private final Duration observabilityRetention;

    ///..
    private final HttpProxyConfiguration[] httpProxyServers;

    ///..
    private final ProxyConfiguration[] tcpServers;

    ///..
    private final int rateLimitAmount;
    private final String[] illegalPathContains;

    ///
    public ApplicationProperties(final String environment) throws IOException, IllegalArgumentException {

        final Properties properties = new Properties();
        final String fileName = "application-" + String.valueOf(environment).toLowerCase(Locale.US) + ".properties";
        final InputStream propertiesFile = ApplicationProperties.class.getClassLoader().getResourceAsStream(fileName);

        if(propertiesFile == null) throw new IllegalArgumentException("Properties file '" + fileName + "' not found");
        properties.load(propertiesFile);

        this.sslCertificate = Path.of(this.resolveProperty(properties, "sslCertificatePath", String.class, false));
        this.sslKeyStorePassword = this.resolveProperty(properties, "sslKeyStorePassword", String.class, false);
        this.siteServers = this.parseAddresses(this.resolveProperty(properties, "siteServers", String.class, false));
        this.siteResources = Path.of(this.resolveProperty(properties, "siteResourcesPath", String.class, false));

        this.siteAllowedOrigins = GenericUtils.fastSplit(this.resolveProperty(properties, "siteAllowedOrigins", String.class, false), ARRAY_SEPARATOR)

            .stream()
            .collect(Collectors.toSet())
        ;

        this.siteDiskBasedResourcesPathPrefix = this.resolveProperty(properties, "siteDiskBasedResourcesPathPrefix", String.class, true);
        this.dashboardServers = this.parseAddresses(this.resolveProperty(properties, "dashboardServers", String.class, false));
        this.dashboardResources = Path.of(this.resolveProperty(properties, "dashboardResourcesPath", String.class, false));

        this.dashboardAllowedOrigins = GenericUtils.fastSplit(this.resolveProperty(properties, "dashboardAllowedOrigins", String.class, false), ARRAY_SEPARATOR)

            .stream()
            .collect(Collectors.toSet())
        ;

        this.loginPassword = this.resolveProperty(properties, "loginPassword", String.class, false);
        this.cookieProperties = this.resolveProperty(properties, "cookieProperties", String.class, false);

        final int observabilityRetentionDays = this.resolveProperty(properties, "observabilityRetentionDays", Integer.class, false);
        if(observabilityRetentionDays <= 0) throw new IllegalArgumentException("Observability retention days must be greater than 0");
        this.observabilityRetention = Duration.ofDays(observabilityRetentionDays);

        this.httpProxyServers = this.parseHttpProxies(this.resolveProperty(properties, "httpProxyServers", String.class, true));
        this.tcpServers = this.parseProxies(this.resolveProperty(properties, "tcpServers", String.class, true));

        this.rateLimitAmount = this.resolveProperty(properties, "rateLimitAmount", Integer.class, false);
        if(this.rateLimitAmount <= 0) throw new IllegalArgumentException("Rate limit amount must be greater than 0");

        this.illegalPathContains = this.parseList(this.resolveProperty(properties, "illegalPathContains", String.class, true));
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
        if(propertyValue.isEmpty()) this.cast(null, clazz, true);

        if(propertyValue.charAt(0) == '$') {

            final String envName = propertyValue.substring(1);
            propertyValue = System.getenv(envName);

            if(propertyValue == null) throw new IllegalArgumentException("The property '" + propertyName + "' must be defined");
            if(!isEmptyAllowed && propertyValue.isEmpty()) throw new IllegalArgumentException("The property '" + propertyName + "' cannot be empty");
        }

        return this.cast(propertyValue, clazz, false);
    }

    ///..
    private InetSocketAddress[] parseAddresses(final String data) {

        if(data == null || data.isEmpty()) return new InetSocketAddress[0];

        final List<String> splits = GenericUtils.fastSplit(data, ARRAY_SEPARATOR);
        final int length = splits.size();
        final InetSocketAddress[] addresses = new InetSocketAddress[length];

        for(int i = 0; i < length; i++) {

            final List<String> components = GenericUtils.fastSplit(splits.get(i), FIELD_SEPARATOR);
            if(components.size() != 2) throw new IllegalArgumentException("Addresses must be composed of ip and port");

            addresses[i] = InetSocketAddress.createUnresolved(components.get(0), Integer.parseInt(components.get(1)));
        }

        return addresses;
    }

    ///..
    private HttpProxyConfiguration[] parseHttpProxies(final String data) {

        if(data == null || data.isEmpty()) return new HttpProxyConfiguration[0];

        final List<String> splits = GenericUtils.fastSplit(data, ARRAY_SEPARATOR);
        final int length = splits.size();
        final HttpProxyConfiguration[] proxies = new HttpProxyConfiguration[length];

        for(int i = 0; i < length; i++) {

            final List<String> components = GenericUtils.fastSplit(splits.get(i), FIELD_SEPARATOR);
            if(components.size() != 4) throw new IllegalArgumentException("Proxies must be composed of 2 ip and port couples");

            proxies[i] = new HttpProxyConfiguration(

                components.get(0),
                InetSocketAddress.createUnresolved(components.get(1), Integer.parseInt(components.get(2))),
                Boolean.parseBoolean(components.get(3))
            );
        }

        return proxies;
    }

    ///..
    private ProxyConfiguration[] parseProxies(final String data) {

        if(data == null || data.isEmpty()) return new ProxyConfiguration[0];

        final List<String> splits = GenericUtils.fastSplit(data, ARRAY_SEPARATOR);
        final int length = splits.size();
        final ProxyConfiguration[] proxies = new ProxyConfiguration[length];

        for(int i = 0; i < length; i++) {

            final List<String> components = GenericUtils.fastSplit(splits.get(i), FIELD_SEPARATOR);
            if(components.size() != 4) throw new IllegalArgumentException("Proxies must be composed of 2 ip and port couples");

            proxies[i] = new ProxyConfiguration(

                InetSocketAddress.createUnresolved(components.get(0), Integer.parseInt(components.get(1))),
                InetSocketAddress.createUnresolved(components.get(2), Integer.parseInt(components.get(3)))
            );
        }

        return proxies;
    }

    ///..
    private String[] parseList(final String data) {

        if(data == null || data.isEmpty()) return new String[0];
        return GenericUtils.fastSplit(data, ARRAY_SEPARATOR).stream().toArray(String[]::new);
    }

    ///..
    private <T> T cast(final String value, final Class<T> clazz, final boolean isDefault) {

        if(isDefault) {

            if(clazz == String.class) return clazz.cast("");
            if(clazz == Integer.class) return clazz.cast(0);
            if(clazz == Boolean.class) return clazz.cast(false);
        }

        if(clazz == String.class) return clazz.cast(value);
        if(clazz == Integer.class) return clazz.cast(Integer.parseInt(value));
        if(clazz == Boolean.class) return clazz.cast(Boolean.parseBoolean(value));

        throw new IllegalArgumentException("Unsupported class '" + clazz + "'");
    }

    ///
}
