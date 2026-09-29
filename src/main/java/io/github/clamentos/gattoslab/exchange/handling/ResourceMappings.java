package io.github.clamentos.gattoslab.exchange.handling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.filters.components.AuthorizationAction;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.exchange.handling.components.StaticResource;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.ResourceWalker;

///..
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
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
    public ResourceMappings(final ApplicationProperties applicationProperties) throws IOException {

        this.apiMappings = this.populateApis();

        this.staticResourcesMappings = this.populateResources(

            applicationProperties.getPrivilegedStaticResourcePathPrefix(),
            applicationProperties.getDiskBasedStaticResourcePathSegment()
        );
    }

    ///
    public Resource get(final String path) {

        final Resource resource = this.staticResourcesMappings.get(path);
        if(resource != null) return resource;

        return this.apiMappings.get(path);
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
    private Map<String, StaticResource> populateResources(

        final String privilegedStaticResourcePathPrefix,
        final String diskBasedStaticResourcePathSegment

    ) throws IOException {

        final Logger logger = new Logger();

        final String[] sitePaths = ResourceWalker.listSiteResourcePaths(ApplicationProperties.STATIC_SITE_RESOURCES_FOLDER);
        final int pathSubstringIndex = ApplicationProperties.STATIC_SITE_RESOURCES_FOLDER.length();
        final Map<String, StaticResource> mappings = HashMap.newHashMap(sitePaths.length + 2);

        long siteSize = 0;

        for(final String prefixedPath : sitePaths) {

            final String path = prefixedPath.substring(pathSubstringIndex);
            final boolean isPublic = !path.contains(privilegedStaticResourcePathPrefix);
            final String diskPath = path.contains(diskBasedStaticResourcePathSegment) ? prefixedPath : null;
            final byte[] compressedContent = diskPath == null ? this.compress(prefixedPath) : null;

            MimeType mimeType = MimeType.decode(GenericUtils.fastSplit(path, '.').getLast());

            if(mimeType == null) {

                logger.warning("Could not decode the mime type for '" + path + "', defaulting to TEXT");
                mimeType = MimeType.TEXT;
            }

            mappings.put(path, new StaticResource(

                isPublic ? AuthorizationAction.ALLOW : AuthorizationAction.REDIRECT,
                mimeType,
                isPublic,
                diskPath,
                compressedContent
            ));

            if(compressedContent != null) siteSize += compressedContent.length;
        }

        final StaticResource homepage = mappings.get(ApplicationProperties.HOMEPAGE_PATH);

        mappings.put("", homepage);
        mappings.put("/", homepage);

        logger.info("Site size: " + GenericUtils.formatSize(siteSize));
        return mappings;
    }

    ///..
    private byte[] compress(final String resourceName) throws IOException {

        if(resourceName == null || resourceName.isEmpty()) throw new IOException("'resourceName' cannot be null");

        final InputStream resourceStream = ResourceMappings.class.getClassLoader().getResourceAsStream(resourceName);
        if(resourceStream == null) throw new IOException("Resource '" + resourceName + "' not found");

        final ByteArrayOutputStream resourceBytes = new ByteArrayOutputStream();
        final GZIPOutputStream compressor = new GZIPOutputStream(resourceBytes);

        compressor.write(resourceStream.readAllBytes());
        compressor.close();

        return resourceBytes.toByteArray();
    }

    ///
}
