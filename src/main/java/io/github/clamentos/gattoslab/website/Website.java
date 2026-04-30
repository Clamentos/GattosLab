package io.github.clamentos.gattoslab.website;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
import io.github.clamentos.gattoslab.utils.ResourceWalker;

///..
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

///..
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class Website {

    ///
    @Getter private final String cacheDuration;
    @Getter private final OffsetDateTime timeAtStartup;
    @Getter private final String timeAtStartupStr;

    ///..
    private final Map<String, WebsiteResource> websiteStructure;

    ///
    public Website(final ApplicationProperties applicationProperties) throws IOException {

        cacheDuration = Long.toString(applicationProperties.getSiteCacheDuration().toSeconds());
        timeAtStartup = OffsetDateTime.now();
        timeAtStartupStr = DateTimeFormatter.RFC_1123_DATE_TIME.format(timeAtStartup);

        final String siteRoot = applicationProperties.getSiteRoot();
        final Set<HttpMethod> supportedGetMethod = Set.of(HttpMethod.GET);
        final Set<HttpMethod> supportedPostMethod = Set.of(HttpMethod.POST);

        websiteStructure = new HashMap<>();
        log.info("Loading and compressing the site into memory...");

        long uncompressedSize = 0;
        long compressedSize = 0;

        for(final String path : ResourceWalker.listSiteResourcePaths(siteRoot)) {

            if(path.contains(".")) {

                final String adjustedPath = path.contains(siteRoot) ? path.substring(siteRoot.length()) : File.separator + path;
                final byte[] content = ResourceWalker.getResource(siteRoot + adjustedPath).readAllBytes();
                final byte[] compressedContent = this.compress(content);
                final boolean isCacheable = !adjustedPath.startsWith("/admin");

                uncompressedSize += content.length;
                compressedSize += compressedContent.length;

                websiteStructure.put(adjustedPath, new WebsiteResource(adjustedPath, this.getMediaType(adjustedPath), compressedContent, supportedGetMethod, isCacheable));
            }
        }

        websiteStructure.put(Apis.FE_ROOT, new WebsiteResource(Apis.FE_ROOT, websiteStructure.get(Apis.FE_INDEX)));

        this.addPath(websiteStructure, Apis.AUTH_ENDPOINT, Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));
        this.addPath(websiteStructure, Apis.REQUEST_METRICS_ENDPOINT, supportedPostMethod);
        this.addPath(websiteStructure, Apis.INVOCATION_METRICS_ENDPOINT, supportedPostMethod);
        this.addPath(websiteStructure, Apis.SYSTEM_METRICS_ENDPOINT, supportedPostMethod);
        this.addPath(websiteStructure, Apis.SESSION_METADATA_ENDPOINT, supportedGetMethod);
        this.addPath(websiteStructure, Apis.LOGS_ENDPOINT, supportedPostMethod);
        this.addPath(websiteStructure, Apis.FALLBACK_LOGS_ENDPOINT, supportedGetMethod);

        log.info("Loading and compressing the site into memory complete. Before: {}, after: {}", uncompressedSize, compressedSize);
        log.info("Website structure: {}", websiteStructure.toString());
    }

    ///
    public WebsiteResource getContent(final String path) {

        return websiteStructure.get(path);
    }

    ///..
    public Set<String> getPaths() {

        return websiteStructure.keySet();
    }

    ///.
    private MimeType getMediaType(final String path) {

        for(final MimeType mimeType : MimeType.values()) {

            if(path.endsWith(mimeType.name().toLowerCase())) return mimeType;
        }

        log.warn("Could not find the appropriate media type for {}. Defaulting to text/plain", path);
        return MimeType.TXT;
    }

    ///..
    private byte[] compress(final byte[] content) throws IOException {

        final ByteArrayOutputStream compressedContent = new ByteArrayOutputStream();
        final CompressingOutputStream outputStream = new CompressingOutputStream(compressedContent, 9);

        outputStream.write(content);
        outputStream.close();

        return compressedContent.toByteArray();
    }

    ///..
    private void addPath(final Map<String, WebsiteResource> websiteStructure, final String path, final Set<HttpMethod> supportedMethods) {

        websiteStructure.put(path, new WebsiteResource(path, MimeType.JSON, null, supportedMethods, false));
    }

    ///
}
