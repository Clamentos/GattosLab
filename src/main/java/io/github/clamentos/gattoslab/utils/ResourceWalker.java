package io.github.clamentos.gattoslab.utils;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

///.
import lombok.Getter;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

///
@Component
@Getter

///
public final class ResourceWalker {

    ///
    private final String pathDelimiter;

    ///
    @Autowired
    public ResourceWalker(final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        if(propertyProvider != null) {

            pathDelimiter = propertyProvider.getProperty("spring.profiles.active", String.class).equals("prod") ? "/" : File.separator;
        }

        else {

            pathDelimiter = "/";
        }
    }

    ///
    public List<String> listSiteResourcePaths(final String start, final PathMatchingResourcePatternResolver resolver)
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
