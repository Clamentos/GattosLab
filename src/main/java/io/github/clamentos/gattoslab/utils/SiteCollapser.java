package io.github.clamentos.gattoslab.utils;

///
import io.github.clamentos.gattoslab.datastructures.MutableString;

///..
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

///..
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

///
public class SiteCollapser {

    ///
    private static final Path SOURCE = Path.of("./src/main/html");
    private static final Path INTERMEDIATE = Path.of("./merged_site");
    private static final Path DESTINATION = Path.of("./site");

    ///..
    private static final int BLACKHOLE_CHUNK_SIZE = 1000;
    private static final int BLACKHOLE_NUM_CHUNKS = 10_000_000;

    ///
    public static void main(final String[] args) throws IOException, InterruptedException {

        System.out.println("SiteCollapser BEGIN");

        deleteDirectory(DESTINATION);
        deleteDirectory(INTERMEDIATE);

        try(final Stream<Path> stream = Files.walk(SOURCE)) {

            final Path[] paths = stream

                .filter(Files::isRegularFile)
                .filter(p -> !p.toString().contains("blackhole.html"))
                .toArray(Path[]::new)
            ;

            for(final Path path : paths) {

                processFile(path);
            }
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(

            "html-minifier-next",
            "--input-dir", INTERMEDIATE.toString(),
            "--output-dir", DESTINATION.toString(),
            "--config-file=./resources/minifier-config.json"
        );

        processBuilder.redirectErrorStream(true);

        final Process process = processBuilder.start();
        final String output = new String(process.getInputStream().readAllBytes());
        final int exitCode = process.waitFor();

        System.out.println("SiteCollapser :: html-minifier-next :: " + output);

        if(exitCode == 0) {

            try(final Stream<Path> stream = Files.walk(INTERMEDIATE)) {

                final Path[] paths = stream.filter(Files::isRegularFile).toArray(Path[]::new);

                for(final Path path : paths) {

                    finalPass(path);
                }
            }

            deleteDirectory(INTERMEDIATE);
        }

        System.out.println("SiteCollapser :: blackhole");
        createBlackhole();

        System.out.println("SiteCollapser END");
        if(exitCode != 0) System.exit(1);
    }

    ///.
    private static void processFile(final Path path) throws IOException {

        final byte[] data = Files.readAllBytes(path);

        if(hasExtension(path, ".html")) placeFile(path, modifyHtml(path, data));
        else if(!hasExtension(path, ".css") && !hasExtension(path, ".js") && !hasExtension(path, ".svg")) placeFile(path, data);
    }

    ///..
    private static void finalPass(final Path path) throws IOException {

        if(!hasExtension(path, ".html")) {

            final Path destinationPath = Path.of(DESTINATION + "/" + path.subpath(2, path.getNameCount()).toString());

            Files.createDirectories(destinationPath.getParent());
            Files.copy(path, destinationPath);
        }
    }

    ///..
    private static byte[] modifyHtml(final Path htmlPath, final byte[] data) throws IOException {

        final Document html = Jsoup.parse(new String(data));
        html.outputSettings().prettyPrint(false);

        concatenateCss(html, htmlPath);
        concatenateSvg(html, htmlPath);
        concatenateJs(html, htmlPath);

        return html.toString().getBytes();
    }

    ///..
    private static void placeFile(final Path sourcePath, final byte[] content) throws IOException {

        final Path destinationPath = Path.of(INTERMEDIATE.toString() + "/" + sourcePath.subpath(4, sourcePath.getNameCount()).toString());

        Files.createDirectories(destinationPath.getParent());
        Files.createFile(destinationPath);

        try(final FileOutputStream os = new FileOutputStream(destinationPath.toFile())) {

            os.write(content);
        }
    }

    ///..
    private static Path getPath(final Path hook, final String path) {

        String temp = path;
        Path result = hook;

        while(temp.startsWith("../")) {

            temp = temp.substring(3);
            result = result.getParent();
        }

        return Path.of(result.resolve(temp).toString());
    }

    ///..
    private static void concatenateCss(final Document html, final Path htmlPath) throws IOException {

        final MutableString sb = new MutableString(16);

        for(final Element stylesheetElem : html.getElementsByAttributeValue("rel", "stylesheet")) {

            final String cssRef = stylesheetElem.attr("href");

            sb.append(new String(Files.readAllBytes(getPath(htmlPath.getParent(), cssRef))));
            stylesheetElem.remove();
        }

        final Element styleTag = html.createElement("style");
        final Element htmlHead = html.head();

        styleTag.text(sb.toString());
        htmlHead.appendChild(styleTag);
    }

    ///..
    private static void concatenateSvg(final Document html, final Path htmlPath) throws IOException {

        final List<Element> imgs = html.getElementsByTag("img").stream().filter(e -> e.attribute("src").getValue().endsWith(".svg")).toList();
        final Encoder encoder = Base64.getEncoder();

        for(final Element img : imgs) {

            final byte[] svgB64 = Files.readAllBytes(getPath(htmlPath.getParent(), img.attr("src")));
            img.attr("src", "data:image/svg+xml;utf8;base64, " + new String(encoder.encode(svgB64)));
        }
    }

    ///..
    private static void concatenateJs(final Document html, final Path htmlPath) throws IOException {

        final List<Element> scripts = html.getElementsByTag("script").stream().toList();

        for(final Element script : scripts) {

            final String jsRef = script.attr("src");
            final String jsSource = new String(Files.readAllBytes(getPath(htmlPath.getParent(), jsRef)));

            script.removeAttr("src");
            script.text(jsSource);
        }
    }

    ///..
    private static boolean hasExtension(final Path path, final String extension) {

        final String pathString = path.toString();
        return pathString.endsWith(extension);
    }

    ///..
    private static void deleteDirectory(final Path root) throws IOException {

        try(final Stream<Path> stream = Files.walk(root)) {

            for(final Path path : stream.sorted(Comparator.reverseOrder()).toList()) {

                Files.delete(path);
            }
        }

        catch(final NoSuchFileException _) {

            System.out.println("SiteCollapser: Directory was already not present: " + root);
        }
    }

    ///..
    private static void createBlackhole() throws IOException {

        final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final int index = (int)Math.abs(System.currentTimeMillis() % alphabet.length());
        final byte[] chunk = Character.toString(alphabet.charAt(index)).repeat(BLACKHOLE_CHUNK_SIZE).getBytes();
        final Path path = Path.of(DESTINATION.toString() + "/blackhole.html");

        try(final GZIPOutputStream compressor = new GZIPOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE))) {

            compressor.write("<!DOCTYPE html><html lang = \"en\">".getBytes());

            for(int i = 0; i < BLACKHOLE_NUM_CHUNKS; i++) {

                compressor.write(chunk);
            }

            compressor.write("</html>".getBytes());
            compressor.finish();
        }
    }

    ///
}
