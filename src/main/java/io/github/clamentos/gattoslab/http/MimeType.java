package io.github.clamentos.gattoslab.http;

///
import java.util.List;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum MimeType {

    ///
    HTML(List.of("text/html")),
    CSS(List.of("text/css")),
    PNG(List.of("image/png")),
    JPG(List.of("image/jpg")),
    JPEG(List.of("image/jpeg")),
    SVG(List.of("image/svg+xml")),
    XML(List.of("application/xml")),
    TEXT(List.of("text/plain")),
    ICO(List.of("image/x-icon")),
    GIF(List.of("image/gif")),
    JS(List.of("application/javascript")),
    JSON(List.of("application/json"));

    ///
    public static MimeType decode(final String fileExtension) {

        if(fileExtension == null) return null;

        switch(fileExtension.toUpperCase()) {

            case "HTML": return MimeType.HTML;
            case "CSS": return MimeType.CSS;
            case "PNG": return MimeType.PNG;
            case "JPG": return MimeType.JPG;
            case "JPEG": return MimeType.JPEG;
            case "SVG": return MimeType.SVG;
            case "XML": return MimeType.XML;
            case "TXT": return MimeType.TEXT;
            case "ICO": return MimeType.ICO;
            case "GIF": return MimeType.GIF;
            case "JS": return MimeType.JS;
            case "JSON": return MimeType.JSON;

            default: return null;
        }
    }

    ///
    private final List<String> mimeValue;

    ///
}
