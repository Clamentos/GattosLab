package io.github.clamentos.gattoslab.web;

///
import io.github.clamentos.gattoslab.utils.Pair;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

///.
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

///
@Service
@Slf4j

///
public final class StaticSite {

    ///
    private final Map<String, String> supportedExtensions;
    private final int cacheDuration;
    final String pathDelimiter;

    @Getter
    private final OffsetDateTime timeAtStartup;

    ///..
    private final Map<String, Pair<String, byte[]>> website;
    private final Set<String> apiPaths;

    ///
    @Autowired
    public StaticSite(final PropertyProvider propertyProvider) throws IOException, PropertyNotFoundException {

        supportedExtensions = new HashMap<>();

        for(final String split : propertyProvider.getProperty("app.site.supportedMimeTypes", String.class).split(",")) {

            final String[] subSplits = split.split("\\|");
            supportedExtensions.put(subSplits[0], subSplits[1]);
        }

        cacheDuration = propertyProvider.getProperty("app.site.cacheDuration", Integer.class) * 60;
        pathDelimiter = propertyProvider.getProperty("spring.profiles.active", String.class).equals("prod") ? "/" : File.separator;
        timeAtStartup = OffsetDateTime.now();

        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        website = new HashMap<>();

        for(final String path : this.listSiteResourcePaths("site", resolver)) {

            if(path.contains(".")) {

                final String adjustedPath = path.contains("site") ? path.substring(4) : pathDelimiter + path;
                final byte[] content = new ClassPathResource("site" + adjustedPath).getInputStream().readAllBytes();

                website.put(adjustedPath, new Pair<>(this.getMediaType(adjustedPath), content));
            }
        }

        website.put("/", website.get("/index.html"));
        log.info("Website resource paths: {}", website.keySet());

        apiPaths = new HashSet<>();
        apiPaths.add("/admin/api/session");
        apiPaths.add("/admin/api/observability/paths-count");
        apiPaths.add("/admin/api/observability/user-agents-count");
        apiPaths.add("/admin/api/observability/performance-charts");
        apiPaths.add("/admin/api/observability/jvm-metrics");
        apiPaths.add("/admin/api/observability/sessions-metadata");
        apiPaths.add("/admin/api/observability/logs");

        log.info("Website API paths: {}", apiPaths);
    }

    ///
    public Pair<String, byte[]> getContent(final String path) {

        return website.get(path);
    }

    ///..
    public Set<String> getPaths() {

        final Set<String> result = new HashSet<>(website.keySet());
        result.addAll(apiPaths);

        return result;
    }

    ///..
    public Set<String> getPaths(final String basePath) {

        return this.getPaths().stream().filter(p -> p.startsWith(basePath)).collect(Collectors.toCollection(HashSet::new));
    }

    ///..
    public ResponseEntity<byte[]> buildSiteResponse(final HttpStatusCode status, final Pair<String, byte[]> content) {

        return ResponseEntity

            .status(status)
            .header("Content-Type", content.getA())
            .header("Cache-Control", "max-age=" + cacheDuration + ", public")
            .header("Last-Modified", DateTimeFormatter.RFC_1123_DATE_TIME.format(timeAtStartup))
            .body(content.getB())
        ; 
    }

    ///.
    private String getMediaType(final String path) {

        for(final Map.Entry<String, String> entry : supportedExtensions.entrySet()) {

            if(path.contains(entry.getKey())) return entry.getValue();
        }

        log.warn("Could not find the appropriate media type for {}. Defaulting to text/plain", path);
        return "text/plain";
    }

    ///..
    private List<String> listSiteResourcePaths(final String start, final PathMatchingResourcePatternResolver resolver)
    throws IOException {

        final List<String> siteResourceNames = this.getResourcesNamesIn(start, resolver);

        for(int i = 0; i < siteResourceNames.size(); i++) {

            if(!siteResourceNames.get(i).contains(".")) {

                siteResourceNames.addAll(this.listSiteResourcePaths(siteResourceNames.get(i), resolver));
            }
        }

        return siteResourceNames;
    }

    ///..
    private List<String> getResourcesNamesIn(final String path, final PathMatchingResourcePatternResolver resolver)
    throws IOException {

        final String rawRootUri = this.getResource(path, resolver).getURI().toString();
        final String rootUri = URLDecoder.decode(rawRootUri.endsWith(pathDelimiter) ? rawRootUri : rawRootUri + pathDelimiter, "UTF-8");
        final int rootUriLength = rootUri.length();

        final List<Resource> resources = this.getResourcesIn(path, resolver);
        final List<String> resourceNames = new ArrayList<>(resources.size());

        for(final Resource resource : resources) {

            final String uri = URLDecoder.decode(resource.getURI().toString(), "UTF-8");
            final boolean isFile = uri.indexOf(pathDelimiter, rootUriLength) == -1;

            if(isFile) resourceNames.add(path + pathDelimiter + uri.substring(rootUriLength));
            else resourceNames.add(path + pathDelimiter + uri.substring(rootUriLength, uri.indexOf(pathDelimiter, rootUriLength + 1)));
        }

        return resourceNames;
    }

    ///..
    private List<Resource> getResourcesIn(final String path, final PathMatchingResourcePatternResolver resolver) throws IOException {

        final Resource root = this.getResource(path, resolver);
        final String rootUri =  root.getURI().toString();
        final int rootUriLength = rootUri.length();
        final String pathPattern = (path.endsWith(pathDelimiter)) ? path + "**" : path + pathDelimiter + "**";

        final Resource[] resources = this.getResources(pathPattern, resolver);
        final List<Resource> children = new ArrayList<>();

        for(final Resource resource : resources) {

            final String uri = resource.getURI().toString();
            final int uriLength = uri.length();
            final boolean isChild = uriLength > rootUriLength && !uri.equals(rootUri + pathDelimiter);

            if(isChild) {

                final boolean isDirInside = uri.indexOf(pathDelimiter, rootUriLength + 1) == uriLength - 1;
                final boolean isFileInside = uri.indexOf(pathDelimiter, rootUriLength + 1) == -1;

                if(isDirInside || isFileInside) children.add(resource);
            }
        }

        return children;
    }

    ///..
    private Resource getResource(final String path, final PathMatchingResourcePatternResolver resolver) {

        return resolver.getResource(path.replace("\\", pathDelimiter));
    }

    ///..
    private Resource[] getResources(String path, final PathMatchingResourcePatternResolver resolver) throws IOException {

        return resolver.getResources(path.replace("\\", pathDelimiter));
    }

    ///
}
