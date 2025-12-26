package io.github.clamentos.gattoslab.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.Base64.Encoder;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SiteCollapser {

    // html-minifier-next --input-dir ./src/main/resources/site-src --output-dir ./src/main/resources/minified --config-file=./src/main/resources/minifier-config.json

    private static final String SOURCE_ROOT = "minified";
    private static final String DESTINATION_ROOT = "site";

    public static void main(String[] args) throws IOException {

        final ResourceWalker resourceWalker = new ResourceWalker();
        final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        final Set<String> paths = resourceWalker.listSiteResourcePaths(SOURCE_ROOT, resolver)

            .stream()
            .filter(p -> p.contains("."))
            .collect(Collectors.toSet())
        ;

        for(final String path : paths) {

            processFile(path);
        }
    }

    private static void processFile(final String path) throws IOException {

        final byte[] data = new ClassPathResource(path).getInputStream().readAllBytes();

        if(path.endsWith(".html")) placeFile(path, modifyHtml(path, data));
        else if(!path.endsWith(".css") && !path.endsWith(".js") && !path.endsWith(".svg")) placeFile(path, data);
    }

    private static byte[] modifyHtml(final String path, final byte[] data) throws IOException {

        final Path htmlPath = Path.of(path);
        final Document html = Jsoup.parse(new String(data));

        html.outputSettings().prettyPrint(false);

        concatenateCss(html, htmlPath);
        concatenateSvg(html, htmlPath);
        concatenateJs(html, htmlPath);

        return html.toString().getBytes();
    }

    private static void placeFile(final String sourcePath, final byte[] content) throws IOException {

        final Path destinationPath = Path.of(DESTINATION_ROOT + sourcePath.substring(SOURCE_ROOT.length()));

        Files.createDirectories(destinationPath.getParent());
        Files.createFile(destinationPath);

        try(final FileOutputStream os = new FileOutputStream(destinationPath.toFile())) {

            os.write(content);
        }
    }

    private static String getPath(final Path hook, final String path) {

        String temp = path;
        Path result = hook;

        while(temp.startsWith("../")) {

            temp = temp.substring(3);
            result = result.getParent();
        }

        return result.resolve(temp).toString();
    }

    private static void concatenateCss(final Document html, final Path htmlPath) throws IOException {

        final StringBuilder sb = new StringBuilder();

        for(final Element stylesheetElem : html.getElementsByAttributeValue("rel", "stylesheet")) {

            final String cssRef = stylesheetElem.attr("href");

            sb.append(new String(new ClassPathResource(getPath(htmlPath.getParent(), cssRef)).getInputStream().readAllBytes()));
            stylesheetElem.remove();
        }

        final Element styleTag = html.createElement("style");
        final Element htmlHead = html.head();

        styleTag.text(sb.toString());
        htmlHead.appendChild(styleTag);
    }

    private static void concatenateSvg(final Document html, final Path htmlPath) throws IOException {

        final List<Element> imgs = html.getElementsByTag("img").stream().filter(e -> e.attribute("src").getValue().endsWith(".svg")).toList();
        final Encoder encoder = Base64.getEncoder();

        for(final Element img : imgs) {

            final byte[] svgB64 = new ClassPathResource(getPath(htmlPath.getParent(), img.attr("src"))).getInputStream().readAllBytes();
            img.attr("src", "data:image/svg+xml;utf8;base64, " + new String(encoder.encode(svgB64)));
        }
    }

    private static void concatenateJs(final Document html, final Path htmlPath) throws IOException {

        final List<Element> scripts = html.getElementsByTag("script").stream().toList();

        for(final Element script : scripts) {

            final String jsRef = script.attr("src");
            final String jsSource = new String(new ClassPathResource(getPath(htmlPath.getParent(), jsRef)).getInputStream().readAllBytes());

            script.removeAttr("src");
            script.text(jsSource);
        }
    }
}
