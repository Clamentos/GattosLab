package io.github.clamentos.gattoslab.http;

///
import java.util.Locale;

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
    HTML(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "text/html")),
    CSS(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "text/css")),
    PNG(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "image/png")),
    JPG(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "image/jpg")),
    JPEG(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "image/jpeg")),
    SVG(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "image/svg+xml")),
    XML(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "application/xml")),
    TEXT(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "text/plain")),
    ICO(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "image/x-icon")),
    GIF(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "image/gif")),
    JS(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "application/javascript")),
    JSON(new HttpHeader(HttpHeaderName.CONTENT_TYPE, "application/json"));

    ///
    private final HttpHeader valueForResponse;

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
