package io.github.clamentos.gattoslab.web;

///
import io.github.clamentos.gattoslab.utils.Pair;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.io.IOException;
import java.net.URLDecoder;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final Map<String, Pair<String, byte[]>> website;

    ///..
    private final Map<String, String> supportedExtensions;
    private final int cacheDuration;

    @Getter
    private final DateTimeFormatter dateTimeFormatter;

    @Getter
    private final OffsetDateTime timeAtStartup;

    ///
    @SuppressWarnings("squid:S1075")
    @Autowired
    public StaticSite(final PropertyProvider propertyProvider) throws IOException, PropertyNotFoundException {

        website = new HashMap<>();
        supportedExtensions = new HashMap<>();

        supportedExtensions.put("html", "text/html");
        supportedExtensions.put("css", "text/css");
        supportedExtensions.put("png", "image/png");
        supportedExtensions.put("jpg", "image/jpg");
        supportedExtensions.put("jpeg", "image/jpeg");
        supportedExtensions.put("svg", "image/svg+xml");
        supportedExtensions.put("webp", "image/webp");
        supportedExtensions.put("xml", "application/xml");
        supportedExtensions.put("txt", "text/plain");
        supportedExtensions.put("ico", "image/x-icon");
        supportedExtensions.put("gif", "image/gif");

        cacheDuration = propertyProvider.getProperty("app.site.cacheDuration", Integer.class);
        dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        timeAtStartup = OffsetDateTime.now(); // ENHANCEMENT: would be nice to actually get the jar modification date

        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for(final String path : this.listSiteResourcePaths("site", resolver)) {

            if(path.contains(".")) {

                final String adjustedPath = path.contains("site") ? path.substring(4) : "/" + path;
                final byte[] content = new ClassPathResource("site" + adjustedPath).getInputStream().readAllBytes();

                website.put(adjustedPath, new Pair<>(this.getMediaType(adjustedPath), content));
            }
        }

        website.put("/", website.get("/index.html"));
        log.info("Website resource paths: {}", website.keySet());
    }

    ///
    public Pair<String, byte[]> getContent(final String path) {

        return website.get(path);
    }

    ///..
    public Set<String> getSitePaths() {

        return website.keySet();
    }

    ///..
    public ResponseEntity<byte[]> buildSiteResponse(final HttpStatusCode status, final String contentType, final byte[] content) {

        return ResponseEntity

            .status(status)
            .header("Content-Type", contentType)
            .header("Cache-Control", "max-age=" + cacheDuration + ", public")
            .header("Last-Modified", dateTimeFormatter.format(timeAtStartup))
            .body(content)
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
        final String rootUri = URLDecoder.decode(rawRootUri.endsWith("/") ? rawRootUri : rawRootUri + "/", "UTF-8");
        final int rootUriLength = rootUri.length();

        final List<Resource> resources = this.getResourcesIn(path, resolver);
        final List<String> resourceNames = new ArrayList<>(resources.size());

        for(final Resource resource : resources) {

            final String uri = URLDecoder.decode(resource.getURI().toString(), "UTF-8");
            final boolean isFile = uri.indexOf("/", rootUriLength) == -1;

            if(isFile) resourceNames.add(path + "/" + uri.substring(rootUriLength));
            else resourceNames.add(path + "/" + uri.substring(rootUriLength, uri.indexOf("/", rootUriLength + 1)));
        }

        return resourceNames;
    }

    ///..
    private List<Resource> getResourcesIn(final String path, final PathMatchingResourcePatternResolver resolver) throws IOException {

        final Resource root = this.getResource(path, resolver);
        final String rootUri =  root.getURI().toString();
        final int rootUriLength = rootUri.length();
        final String pathPattern = (path.endsWith("/")) ? path + "**" : path + "/**";

        final Resource[] resources = this.getResources(pathPattern, resolver);
        final List<Resource> children = new ArrayList<>();

        for(final Resource resource : resources) {

            final String uri = resource.getURI().toString();
            final int uriLength = uri.length();
            final boolean isChild = uriLength > rootUriLength && !uri.equals(rootUri + "/");

            if(isChild) {

                final boolean isDirInside = uri.indexOf("/", rootUriLength + 1) == uriLength - 1;
                final boolean isFileInside = uri.indexOf("/", rootUriLength + 1) == -1;

                if(isDirInside || isFileInside) children.add(resource);
            }
        }

        return children;
    }

    ///..
    private Resource getResource(final String path, final PathMatchingResourcePatternResolver resolver) {

        return resolver.getResource(path.replace("\\", "/"));
    }

    ///..
    private Resource[] getResources(String path, final PathMatchingResourcePatternResolver resolver) throws IOException {

        return resolver.getResources(path.replace("\\", "/"));
    }

    ///
}
