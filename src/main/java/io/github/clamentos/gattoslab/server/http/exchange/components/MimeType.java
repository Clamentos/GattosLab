package io.github.clamentos.gattoslab.server.http.exchange.components;

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
    CSS(Map.of(HttpHeader.CONTENT_TYPE, "text/css")),
    GIF(Map.of(HttpHeader.CONTENT_TYPE, "image/gif")),
    HTML(Map.of(HttpHeader.CONTENT_TYPE, "text/html")),
    ICO(Map.of(HttpHeader.CONTENT_TYPE, "image/x-icon")),
    JPEG(Map.of(HttpHeader.CONTENT_TYPE, "image/jpeg")),
    JPG(Map.of(HttpHeader.CONTENT_TYPE, "image/jpg")),
    JS(Map.of(HttpHeader.CONTENT_TYPE, "application/javascript")),
    JSON(Map.of(HttpHeader.CONTENT_TYPE, "application/json")),
    PNG(Map.of(HttpHeader.CONTENT_TYPE, "image/png")),
    SVG(Map.of(HttpHeader.CONTENT_TYPE, "image/svg+xml")),
    TEXT(Map.of(HttpHeader.CONTENT_TYPE, "text/plain")),
    XML(Map.of(HttpHeader.CONTENT_TYPE, "application/xml"));

    ///
    private final Map<HttpHeader, String> responseValue;

    ///
    public static MimeType decode(final String fileExtension) {

        if(fileExtension == null) return null;

        switch(fileExtension.toUpperCase(Locale.US)) {

            case "CSS": return MimeType.CSS;
            case "GIF": return MimeType.GIF;
            case "HTML": return MimeType.HTML;
            case "ICO": return MimeType.ICO;
            case "JPEG": return MimeType.JPEG;
            case "JPG": return MimeType.JPG;
            case "JS": return MimeType.JS;
            case "JSON": return MimeType.JSON;
            case "PNG": return MimeType.PNG;
            case "SVG": return MimeType.SVG;
            case "TXT": return MimeType.TEXT;
            case "XML": return MimeType.XML;

            default: return null;
        }
    }

    ///..
    public static MimeType fromMime(final String mime) {

        if(mime != null) {

            switch(mime) {

                case "text/css": return MimeType.CSS;
                case "image/gif": return MimeType.GIF;
                case "text/html": return MimeType.HTML;
                case "image/x-icon": return MimeType.ICO;
                case "image/jpeg": return MimeType.JPEG;
                case "image/jpg": return MimeType.JPG;
                case "application/javascript": return MimeType.JS;
                case "application/json": return MimeType.JSON;
                case "image/png": return MimeType.PNG;
                case "image/svg+xml": return MimeType.SVG;
                case "text/plain": return MimeType.TEXT;
                case "application/xml": return MimeType.XML;

                default: return null;
            }
        }

        return MimeType.TEXT;
    }

    ///
}
