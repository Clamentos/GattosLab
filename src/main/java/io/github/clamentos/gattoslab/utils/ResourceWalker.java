package io.github.clamentos.gattoslab.utils;

///
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class ResourceWalker {

    ///
    public static InputStream getResource(final String path) {

        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    ///..
    public static List<String> listSiteResourcePaths(final String rootPath) throws IOException {

        final URL url = Thread.currentThread().getContextClassLoader().getResource(rootPath);
        if(url == null) throw new IOException("Could not find the resource at: " + rootPath);

        try {

            final URI uri = url.toURI();

            if ("jar".equals(uri.getScheme())) return listFromJar(uri, rootPath);
            else return listFromFileSystem(uri, rootPath);
        }

        catch(final URISyntaxException exc) {

            throw new IOException(exc);
        }
    }

    ///..
    private static List<String> listFromJar(final URI jarUri, final String rootPath) throws IOException {

        final List<String> result = new ArrayList<>();

        try(FileSystem filesystem = FileSystems.newFileSystem(jarUri, Map.of())) {

            final Path root = filesystem.getPath("/" + rootPath);

            try(final Stream<Path> stream = Files.walk(root)) {

                stream

                    .filter(Files::isRegularFile)
                    .forEach(p -> result.add(rootPath + "/" + root.relativize(p).toString().replace('\\', '/')))
                ;
            }
        }

        return result;
    }

    ///..
    private static List<String> listFromFileSystem(final URI uri, final String rootPath) throws IOException {

        final Path root = Paths.get(uri);

        try(final Stream<Path> stream = Files.walk(root)) {

            return stream

                .filter(Files::isRegularFile)
                .map(root::relativize)
                .map(p -> rootPath + "/" + p.toString().replace('\\', '/'))
                .toList()
            ;
        }
    }

    ///
}
