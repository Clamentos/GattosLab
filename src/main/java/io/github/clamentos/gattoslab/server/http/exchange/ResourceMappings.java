package io.github.clamentos.gattoslab.server.http.exchange;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.components.HttpProxy;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;
import io.github.clamentos.gattoslab.server.http.exchange.components.MimeType;
import io.github.clamentos.gattoslab.server.http.exchange.components.resources.Api;
import io.github.clamentos.gattoslab.server.http.exchange.components.resources.Resource;
import io.github.clamentos.gattoslab.server.http.exchange.components.resources.StaticResource;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

///..
import lombok.Getter;

///
@Getter

///
public final class ResourceMappings {

    ///
    private final Map<String, Map<String, StaticResource>> siteResourcesMappings;
    private final Map<String, StaticResource> dashboardResourcesMappings;

    ///..
    private final Map<String, Pair<HttpProxy, Void>> knownDomains;
    private final Set<String> httpProxyAuthExclusions;

    ///
    public ResourceMappings(final ApplicationProperties applicationProperties) throws IOException {

        final Logger logger = new Logger();
        this.siteResourcesMappings = new HashMap<>();

        for(final Entry<Path, Set<String>> resourceMapping : applicationProperties.getSiteResources().entrySet()) {

            final Map<String, StaticResource> mappings = this.generateSiteMappings(

                logger,
                resourceMapping.getKey(),
                resourceMapping.getValue(),
                applicationProperties.getSiteDiskBasedResourcesPathPrefix()
            );

            for(final String domain : resourceMapping.getValue()) {

                this.siteResourcesMappings.put(domain, mappings);
            }
        }

        this.dashboardResourcesMappings = this.generateDashboardMappings(logger, applicationProperties.getDashboardResourcesPath());
        this.knownDomains = this.generateKnownDomains(applicationProperties.getSiteResources(), applicationProperties.getHttpProxyServers());
        this.httpProxyAuthExclusions = applicationProperties.getHttpProxyAuthExclusions();
    }

    ///
    public Resource getSite(final String domain, final String path) {

        final Map<String, StaticResource> siteResourcesMapping = this.siteResourcesMappings.get(domain);
        if(siteResourcesMapping == null) return null;

        return siteResourcesMapping.get(path);
    }

    ///..
    public Resource getDashboard(final String path) {

        final Resource resource = this.dashboardResourcesMappings.get(path);
        if(resource != null) return resource;

        return Api.decode(path);
    }

    ///..
    public Pair<HttpProxy, Void> getDomainInformation(final String domain) {

        return this.knownDomains.get(domain);
    }

    ///..
    public boolean requiresAuth(final String path) {

        return !this.httpProxyAuthExclusions.contains(path);
    }

    ///.
    private Map<String, StaticResource> generateSiteMappings(

        final Logger logger,
        final Path siteResources,
        final Set<String> domains,
        final String siteDiskBasedResourcesPathPrefix

    ) throws IOException {

        final Path[] sitePaths = this.walk(siteResources);
        final Map<String, StaticResource> mappings = HashMap.newHashMap(sitePaths.length + 2);

        long compressedSiteSize = 0;

        for(final Path prefixedPath : sitePaths) {

            final String path = File.separatorChar + prefixedPath.subpath(2, prefixedPath.getNameCount()).toString();
            final Path diskPath = path.contains(siteDiskBasedResourcesPathPrefix) ? Path.of(path) : null;
            final byte[] compressedContent = diskPath == null ? this.compress(prefixedPath) : null;

            MimeType mimeType = MimeType.decode(GenericUtils.fastSplit(path, '.').getLast());

            if(mimeType == null) {

                logger.warning("Could not decode the mime type for '" + path + "', defaulting to TEXT");
                mimeType = MimeType.TEXT;
            }

            mappings.put(path.replace('\\', '/'), new StaticResource(AuthorizationAction.ALLOW, mimeType, true, diskPath, compressedContent));
            if(compressedContent != null) compressedSiteSize += compressedContent.length;
        }

        final StaticResource homepage = mappings.get(ApplicationProperties.HOMEPAGE_PATH);

        mappings.put("", homepage);
        mappings.put("/", homepage);

        logger.info("Site size for domains: " + domains + ": " + GenericUtils.formatSize(compressedSiteSize));
        return mappings;
    }

    ///..
    private Map<String, StaticResource> generateDashboardMappings(final Logger logger, final Path dashboardResources) throws IOException {

        final Path[] dashboardPaths = this.walk(dashboardResources);
        final Map<String, StaticResource> mappings = HashMap.newHashMap(dashboardPaths.length + 2);

        long compressedSiteSize = 0;

        for(final Path prefixedPath : dashboardPaths) {

            final String path = File.separatorChar + prefixedPath.subpath(2, prefixedPath.getNameCount()).toString();
            final byte[] compressedContent = this.compress(prefixedPath);

            final AuthorizationAction authorizationAction = path.contains(ApplicationProperties.LOGIN_PATH) ?

                AuthorizationAction.ALLOW :
                AuthorizationAction.REDIRECT
            ;

            MimeType mimeType = MimeType.decode(GenericUtils.fastSplit(path, '.').getLast());

            if(mimeType == null) {

                logger.warning("Could not decode the mime type for '" + path + "', defaulting to TEXT");
                mimeType = MimeType.TEXT;
            }

            mappings.put(path.replace('\\', '/'), new StaticResource(authorizationAction, mimeType, false, null, compressedContent));
            compressedSiteSize += compressedContent.length;
        }

        final StaticResource homepage = mappings.get(ApplicationProperties.HOMEPAGE_PATH);

        mappings.put("", homepage);
        mappings.put("/", homepage);

        logger.info("Dashboard size: " + GenericUtils.formatSize(compressedSiteSize));
        return mappings;
    }

    ///..
    private Map<String, Pair<HttpProxy, Void>> generateKnownDomains(

        final Map<Path, Set<String>> siteResources,
        final Set<HttpProxy> httpProxyServers
    ) {

        final Pair<HttpProxy, Void> publicResourcesConfig = new Pair<>(null, null);
        final Map<String, Pair<HttpProxy, Void>> mappings = new HashMap<>();

        for(final Set<String> domains : siteResources.values()) {

            for(final String domain : domains) {

                mappings.put(domain, publicResourcesConfig);
            }
        }

        for(final HttpProxy httpProxyServer : httpProxyServers) {

            mappings.put(httpProxyServer.getDomain(), new Pair<>(httpProxyServer, null));
        }

        return mappings;
    }

    ///..
    private Path[] walk(final Path resources) throws IOException {

        try(final Stream<Path> stream = Files.walk(resources)) {

            return stream.filter(Files::isRegularFile).toArray(Path[]::new);
        }
    }

    ///..
    private byte[] compress(final Path resourcePath) throws IOException {

        try(final InputStream resourceStream = Files.newInputStream(resourcePath)) {

            if(resourceStream == null) throw new IOException("Resource '" + resourcePath + "' not found");

            final ByteArrayOutputStream resourceBytes = new ByteArrayOutputStream();
            final GZIPOutputStream compressor = new GZIPOutputStream(resourceBytes);

            compressor.write(resourceStream.readAllBytes());
            compressor.close();

            return resourceBytes.toByteArray();
        }
    }

    ///
}
