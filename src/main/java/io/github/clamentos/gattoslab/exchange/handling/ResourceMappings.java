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
    public ResourceMappings() throws IOException, IllegalArgumentException {

        final Logger logger = new Logger();

        final Api[] apis =  Api.values();
        this.apiMappings = HashMap.newHashMap(apis.length);

        for(final Api api : apis) {

            this.apiMappings.put(api.getPath(), api);
        }

        final String[] sitePaths = ResourceWalker.listSiteResourcePaths(ApplicationProperties.STATIC_SITE_RESOURCES_FOLDER);
        final int pathSubstringIndex = ApplicationProperties.STATIC_SITE_RESOURCES_FOLDER.length();
        long siteSize = 0;

        this.staticResourcesMappings = HashMap.newHashMap(sitePaths.length + 2);

        for(final String prefixedPath : sitePaths) {

            final String path = prefixedPath.substring(pathSubstringIndex);
            final boolean isPublic = !path.contains(ApplicationProperties.PRIVILEGED_STATIC_RESOURCE_PATH_PREFIX);
            final AuthorizationAction authorizationAction = isPublic ? AuthorizationAction.ALLOW : AuthorizationAction.REDIRECT;
            final String diskPath = path.contains(ApplicationProperties.DISK_BASED_STATIC_RESOURCE_PATH_SEGMENT) ? path : null;
            final byte[] compressedContent = diskPath == null ? this.compress(prefixedPath) : null;

            MimeType mimeType = MimeType.decode(GenericUtils.fastSplit(path, '.').getLast());

            if(mimeType == null) {

                logger.warning("Could not decode MimeType for '" + path + "', defaulting to TEXT");
                mimeType = MimeType.TEXT;
            }

            this.staticResourcesMappings.put(path, new StaticResource(authorizationAction, mimeType, isPublic, diskPath, compressedContent));
            if(compressedContent != null) siteSize += compressedContent.length;
        }

        final StaticResource homepage = this.staticResourcesMappings.get("/index.html");

        this.staticResourcesMappings.put("", homepage);
        this.staticResourcesMappings.put("/", homepage);

        logger.info("Site size: " + GenericUtils.formatSize(siteSize));
    }

    ///
    public Resource get(final String path) {

        Resource resource = this.staticResourcesMappings.get(path);
        if(resource != null) return resource;

        resource = this.apiMappings.get(path);
        return resource;
    }

    ///.
    private byte[] compress(final String resourceName) throws IOException, IllegalArgumentException {

        if(resourceName == null || resourceName.isEmpty()) return null;

        final InputStream resourceStream = ResourceMappings.class.getClassLoader().getResourceAsStream(resourceName);
        if(resourceStream == null) throw new IllegalArgumentException("Resource '" + resourceName + "' not found");

        final ByteArrayOutputStream resourceBytes = new ByteArrayOutputStream();
        final GZIPOutputStream compressor = new GZIPOutputStream(resourceBytes);

        compressor.write(resourceStream.readAllBytes());
        compressor.flush();
        compressor.close();

        return resourceBytes.toByteArray();
    }

    ///
}
