package io.github.clamentos.gattoslab.utils;

///
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public static String[] listSiteResourcePaths(final String rootPath) throws IOException {

        final URL url = Thread.currentThread().getContextClassLoader().getResource(rootPath);
        if(url == null) throw new IOException("Could not find the resource at '" + rootPath + "'");

        try {

            final URI uri = url.toURI();

            if ("jar".equals(uri.getScheme())) return listFromJar(uri, rootPath);
            else return listFromFileSystem(uri, rootPath);
        }

        catch(final URISyntaxException exc) {

            throw new IOException("Could not access URI because", exc);
        }
    }

    ///..
    private static String[] listFromJar(final URI jarUri, final String rootPath) throws IOException {

        try(FileSystem filesystem = FileSystems.newFileSystem(jarUri, Map.of())) {

            final Path root = filesystem.getPath("/" + rootPath);

            try(final Stream<Path> stream = Files.walk(root)) {

                return stream

                    .filter(Files::isRegularFile)
                    .map(p -> rootPath + "/" + root.relativize(p).toString())
                    .toArray(String[]::new)
                ;
            }
        }
    }

    ///..
    private static String[] listFromFileSystem(final URI uri, final String rootPath) throws IOException {

        final Path root = Paths.get(uri);

        try(final Stream<Path> stream = Files.walk(root)) {

            return stream

                .filter(Files::isRegularFile)
                .map(root::relativize)
                .map(p -> rootPath + "/" + p.toString())
                .toArray(String[]::new)
            ;
        }
    }

    ///
}
