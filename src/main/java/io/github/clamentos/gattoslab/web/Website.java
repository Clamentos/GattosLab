package io.github.clamentos.gattoslab.web;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
import io.github.clamentos.gattoslab.utils.ResourceWalker;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

///.
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

///
@Service
@Slf4j

///
public final class Website {

    ///
    private final int cacheDuration;

    @Getter
    private final OffsetDateTime timeAtStartup;

    ///..
    private final Map<String, WebsiteResource> websiteStructure;

    ///
    @Autowired
    public Website(final PropertyProvider propertyProvider, final ResourceWalker resourceWalker) throws IOException, PropertyNotFoundException {

        cacheDuration = propertyProvider.getProperty("app.site.cacheDuration", Integer.class) * 60;
        timeAtStartup = OffsetDateTime.now();

        final Map<String, String> supportedMimeTypes = new HashMap<>();

        for(final String split : propertyProvider.getProperty("app.site.supportedMimeTypes", String.class).split(",")) {

            final String[] subSplits = split.split("\\|");
            supportedMimeTypes.put(subSplits[0], subSplits[1]);
        }

        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        final Set<HttpMethod> supportedGetMethod = Set.of(HttpMethod.GET);
        final Set<HttpMethod> supportedPostMethod = Set.of(HttpMethod.POST);

        websiteStructure = new HashMap<>();
        log.info("Loading and compressing the site into memory...");

        long uncompressedSize = 0;
        long compressedSize = 0;

        for(final String path : resourceWalker.listSiteResourcePaths("site", resolver)) {

            if(path.contains(".")) {

                final String adjustedPath = path.contains("site") ? path.substring(4) : resourceWalker.getPathDelimiter() + path;
                final byte[] content = new ClassPathResource("site" + adjustedPath).getInputStream().readAllBytes();
                final byte[] compressedContent = this.compress(content);

                uncompressedSize += content.length;
                compressedSize += compressedContent.length;

                websiteStructure.put(adjustedPath, new WebsiteResource(

                    adjustedPath,
                    this.getMediaType(adjustedPath, supportedMimeTypes),
                    compressedContent,
                    false,
                    supportedGetMethod
                ));
            }
        }

        websiteStructure.put("/", new WebsiteResource("/", websiteStructure.get("/index.html")));

        this.addPath(websiteStructure, "/admin/api/session", Set.of(HttpMethod.POST, HttpMethod.DELETE));
        this.addPath(websiteStructure, "/admin/api/observability/paths-invocation", supportedPostMethod);
        this.addPath(websiteStructure, "/admin/api/observability/user-agents-count", supportedPostMethod);
        this.addPath(websiteStructure, "/admin/api/observability/request-metrics", supportedPostMethod);
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
    public Set<String> getPaths(final String basePath) {

        return websiteStructure.keySet()

            .stream()
            .filter(p -> {

                if(basePath != null) return p.startsWith(basePath);
                else return true;
            })
            .collect(Collectors.toCollection(HashSet::new))
        ;
    }

    ///..
    public ResponseEntity<Object> buildResponseForStaticContent(final HttpStatusCode status, final WebsiteResource resource) {

        return ResponseEntity

            .status(status)
            .header("Content-Type", resource.getMimeType())
            .header("Cache-Control", "max-age=" + cacheDuration + ", public")
            .header("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(timeAtStartup))
            .header("Content-Encoding", "gzip")
            .body(resource.getContent())
        ; 
    }

    ///.
    private String getMediaType(final String path, final Map<String, String> supportedMimeTypes) {

        for(final Map.Entry<String, String> entry : supportedMimeTypes.entrySet()) {

            if(path.contains(entry.getKey())) return entry.getValue();
        }

        log.warn("Could not find the appropriate media type for {}. Defaulting to text/plain", path);
        return "text/plain";
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

        websiteStructure.put(path, new WebsiteResource(path, "application/json", null, true, supportedMethods));
    }

    ///
}
