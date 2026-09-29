package io.github.clamentos.gattoslab.utils;

///
import io.github.clamentos.gattoslab.datastructures.MutableString;

///..
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;

///..
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

///
public class SiteCollapser {

    // html-minifier-next --input-dir ./src/main/resources/merged --output-dir ./src/main/resources/site --config-file=./resources/minifier-config.json

    ///
    private static final String SOURCE_ROOT = "html";
    private static final String DESTINATION_ROOT = "merged";

    ///
    public static void main() throws IOException {

        for(final String path : ResourceWalker.listSiteResourcePaths(SOURCE_ROOT)) {

            processFile(path);
        }
    }

    ///.
    private static void processFile(final String path) throws IOException {

        final byte[] data = readAllBytes(path);

        if(path.endsWith(".html")) placeFile(path, modifyHtml(path, data));
        else if(!path.endsWith(".css") && !path.endsWith(".js") && !path.endsWith(".svg")) placeFile(path, data);
    }

    ///..
    private static byte[] modifyHtml(final String path, final byte[] data) throws IOException {

        final Path htmlPath = Path.of(path);
        final Document html = Jsoup.parse(new String(data));

        html.outputSettings().prettyPrint(false);

        concatenateCss(html, htmlPath);
        concatenateSvg(html, htmlPath);
        concatenateJs(html, htmlPath);

        return html.toString().getBytes();
    }

    ///..
    private static void placeFile(final String sourcePath, final byte[] content) throws IOException {

        final Path destinationPath = Path.of(DESTINATION_ROOT + sourcePath.substring(SOURCE_ROOT.length()));

        Files.createDirectories(destinationPath.getParent());
        Files.createFile(destinationPath);

        try(final FileOutputStream os = new FileOutputStream(destinationPath.toFile())) {

            os.write(content);
        }
    }

    ///..
    private static String getPath(final Path hook, final String path) {

        String temp = path;
        Path result = hook;

        while(temp.startsWith("../")) {

            temp = temp.substring(3);
            result = result.getParent();
        }

        return result.resolve(temp).toString();
    }

    ///..
    private static void concatenateCss(final Document html, final Path htmlPath) throws IOException {

        final MutableString sb = new MutableString(16);

        for(final Element stylesheetElem : html.getElementsByAttributeValue("rel", "stylesheet")) {

            final String cssRef = stylesheetElem.attr("href");

            sb.append(new String(readAllBytes(getPath(htmlPath.getParent(), cssRef))));
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

            final byte[] svgB64 = readAllBytes(getPath(htmlPath.getParent(), img.attr("src")));
            img.attr("src", "data:image/svg+xml;utf8;base64, " + new String(encoder.encode(svgB64)));
        }
    }

    ///..
    private static void concatenateJs(final Document html, final Path htmlPath) throws IOException {

        final List<Element> scripts = html.getElementsByTag("script").stream().toList();

        for(final Element script : scripts) {

            final String jsRef = script.attr("src");
            final String jsSource = new String(readAllBytes(getPath(htmlPath.getParent(), jsRef)));

            script.removeAttr("src");
            script.text(jsSource);
        }
    }

    ///..
    private static byte[] readAllBytes(final String path) throws IOException {

        return ClassLoader.getSystemResourceAsStream(path).readAllBytes();
    }

    ///
}
