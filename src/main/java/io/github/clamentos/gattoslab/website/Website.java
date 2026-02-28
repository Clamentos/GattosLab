package io.github.clamentos.gattoslab.website;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.SiteConfig;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
import io.github.clamentos.gattoslab.utils.HttpMethod;
import io.github.clamentos.gattoslab.utils.MimeType;
import io.github.clamentos.gattoslab.utils.ResourceWalker;

///..
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
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
    @Getter private final int cacheDuration;
    @Getter private final OffsetDateTime timeAtStartup;

    ///..
    private final Map<String, WebsiteResource> websiteStructure;

    ///
    public Website(final ApplicationProperties applicationProperties) throws IOException {

        final SiteConfig siteConfig = applicationProperties.getSiteConfig();

        cacheDuration = siteConfig.getCacheDuration() * 60;
        timeAtStartup = OffsetDateTime.now();

        final String siteRoot = siteConfig.getRoot();
        final Set<HttpMethod> supportedGetMethod = Set.of(HttpMethod.GET);
        final Set<HttpMethod> supportedPostMethod = Set.of(HttpMethod.POST);

        websiteStructure = new HashMap<>();
        log.info("Loading and compressing the site into memory...");

        final ResourceWalker resourceWalker = new ResourceWalker();
        long uncompressedSize = 0;
        long compressedSize = 0;

        for(final String path : resourceWalker.listSiteResourcePaths(siteRoot)) {

            if(path.contains(".")) {

                final String adjustedPath = path.contains(siteRoot) ? path.substring(siteRoot.length()) : resourceWalker.getPathDelimiter() + path;
                final byte[] content = resourceWalker.getResource(siteRoot + adjustedPath).readAllBytes();
                final byte[] compressedContent = this.compress(content);

                uncompressedSize += content.length;
                compressedSize += compressedContent.length;

                websiteStructure.put(adjustedPath, new WebsiteResource(adjustedPath, this.getMediaType(adjustedPath), compressedContent, false, supportedGetMethod));
            }
        }

        websiteStructure.put("/", new WebsiteResource("/", websiteStructure.get("/index.html")));

        this.addPath(websiteStructure, "/api/session", Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));
        this.addPath(websiteStructure, "/admin/api/observability/request-metrics", supportedPostMethod);
        this.addPath(websiteStructure, "/admin/api/observability/invocation-metrics", supportedPostMethod);
        this.addPath(websiteStructure, "/admin/api/observability/system-metrics", supportedPostMethod);
        this.addPath(websiteStructure, "/admin/api/observability/sessions-metadata", supportedGetMethod);
        this.addPath(websiteStructure, "/admin/api/observability/logs", supportedPostMethod);
        this.addPath(websiteStructure, "/admin/api/observability/fallback-logs", supportedGetMethod);

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

        websiteStructure.put(path, new WebsiteResource(path, MimeType.JSON, null, true, supportedMethods));
    }

    ///
}
