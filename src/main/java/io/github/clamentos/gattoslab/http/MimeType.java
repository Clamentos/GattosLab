package io.github.clamentos.gattoslab.http;

///
import java.util.Locale;
import java.util.Map;

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
    HTML(Map.of(HttpHeader.CONTENT_TYPE, "text/html")),
    CSS(Map.of(HttpHeader.CONTENT_TYPE, "text/css")),
    PNG(Map.of(HttpHeader.CONTENT_TYPE, "image/png")),
    JPG(Map.of(HttpHeader.CONTENT_TYPE, "image/jpg")),
    JPEG(Map.of(HttpHeader.CONTENT_TYPE, "image/jpeg")),
    SVG(Map.of(HttpHeader.CONTENT_TYPE, "image/svg+xml")),
    XML(Map.of(HttpHeader.CONTENT_TYPE, "application/xml")),
    TEXT(Map.of(HttpHeader.CONTENT_TYPE, "text/plain")),
    ICO(Map.of(HttpHeader.CONTENT_TYPE, "image/x-icon")),
    GIF(Map.of(HttpHeader.CONTENT_TYPE, "image/gif")),
    JS(Map.of(HttpHeader.CONTENT_TYPE, "application/javascript")),
    JSON(Map.of(HttpHeader.CONTENT_TYPE, "application/json"));

    ///
    private final Map<HttpHeader, String> valueForResponse;

    ///
    public static MimeType decode(final String fileExtension) {

        if(fileExtension == null) return null;

        switch(fileExtension.toUpperCase(Locale.US)) {

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
}
