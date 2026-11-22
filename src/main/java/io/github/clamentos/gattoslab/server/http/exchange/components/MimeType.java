package io.github.clamentos.gattoslab.server.http.exchange.components;

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
    CSS("text/css"),
    GIF("image/gif"),
    HTML("text/html"),
    ICO("image/x-icon"),
    JPEG("image/jpeg"),
    JPG("image/jpg"),
    JS("application/javascript"),
    JSON("application/json"),
    PNG("image/png"),
    SVG("image/svg+xml"),
    TEXT("text/plain"),
    XML("application/xml");

    ///
    private final String responseValue;

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

    ///
}
