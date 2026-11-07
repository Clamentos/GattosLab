package io.github.clamentos.gattoslab.exchange.handling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.exchange.handling.components.StaticResource;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

///..
import lombok.Getter;

///
@Getter

///
public final class ResourceMappings {

    ///
    private final Map<String, Api> apiMappings;
    private final Map<String, StaticResource> staticResourcesMappings;

    ///
    public ResourceMappings() throws IOException {

        this.apiMappings = this.populateApis();
        this.staticResourcesMappings = this.populateResources();
    }

    ///
    public Resource get(final String path) {

        final Resource resource = this.staticResourcesMappings.get(path);
        if(resource != null) return resource;

        return this.apiMappings.get(path);
    }

    ///..
    public List<StaticResource> getDefenses() {

        final List<StaticResource> defenses = new ArrayList<>();

        for(final Entry<String, StaticResource> staticResourceEntry : this.staticResourcesMappings.entrySet()) {

            if(staticResourceEntry.getKey().contains(ApplicationProperties.DEFENSES_PATH_PREFIX)) {

                defenses.add(staticResourceEntry.getValue());
            }
        }

        return defenses;
    }

    ///.
    private Map<String, Api> populateApis() {

        final Api[] apis =  Api.values();
        final Map<String, Api> mappings = HashMap.newHashMap(apis.length);

        for(final Api api : apis) {

            mappings.put(api.getPath(), api);
        }

        return mappings;
    }

    ///..
    private Map<String, StaticResource> populateResources() throws IOException {

        final Logger logger = new Logger();

        final Path[] sitePaths = this.walk();
        final Map<String, StaticResource> mappings = HashMap.newHashMap(sitePaths.length + 2);

        long compressedSiteSize = 0;

        for(final Path prefixedPath : sitePaths) {

            final String uri = "/" + prefixedPath.subpath(2, prefixedPath.getNameCount()).toString();
            final boolean isPublic = !uri.contains(ApplicationProperties.PRIVILEGED_STATIC_RESOURCES_PATH_PREFIX);
            final AuthorizationAction authorizationAction = isPublic ? AuthorizationAction.ALLOW : AuthorizationAction.REDIRECT;
            final Path diskPath = uri.contains(ApplicationProperties.DISK_BASED_STATIC_RESOURCES_PATH_PREFIX) ? Path.of(uri) : null;
            final byte[] compressedContent = diskPath == null ? this.compress(prefixedPath) : null;

            MimeType mimeType = MimeType.decode(GenericUtils.fastSplit(uri, '.').getLast());

            if(mimeType == null) {

                logger.warning("Could not decode the mime type for '" + uri + "', defaulting to TEXT");
                mimeType = MimeType.TEXT;
            }

            mappings.put(uri, new StaticResource(authorizationAction, mimeType, isPublic, diskPath, compressedContent));
            if(compressedContent != null) compressedSiteSize += compressedContent.length;
        }

        final StaticResource homepage = mappings.get(ApplicationProperties.HOMEPAGE_PATH);

        mappings.put("", homepage);
        mappings.put("/", homepage);

        logger.info("Site size: " + GenericUtils.formatSize(compressedSiteSize));
        return mappings;
    }

    ///..
    private Path[] walk() throws IOException {

        try(final Stream<Path> stream = Files.walk(ApplicationProperties.STATIC_SITE_RESOURCES_PATH)) {

            return stream.filter(Files::isRegularFile).toArray(Path[]::new);
        }
    }

    ///..
    private byte[] compress(final Path resourcePath) throws IOException {

        try(final InputStream resourceStream = Files.newInputStream(resourcePath)) {

            if(resourceStream == null) throw new IOException("Resource '" + resourcePath + "' not found");

            if(resourcePath.toString().contains(ApplicationProperties.BLACKHOLE_PATH)) {

                return resourceStream.readAllBytes();
            }

            final ByteArrayOutputStream resourceBytes = new ByteArrayOutputStream();
            final GZIPOutputStream compressor = new GZIPOutputStream(resourceBytes);

            compressor.write(resourceStream.readAllBytes());
            compressor.close();

            return resourceBytes.toByteArray();
        }
    }

    ///
}
