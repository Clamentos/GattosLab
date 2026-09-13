package io.github.clamentos.gattoslab.http;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum MimeType {

    ///
    HTML("text/html".getBytes()),
    CSS("text/css".getBytes()),
    PNG("image/png".getBytes()),
    JPG("image/jpg".getBytes()),
    JPEG("image/jpeg".getBytes()),
    SVG("image/svg+xml".getBytes()),
    XML("application/xml".getBytes()),
    TEXT("text/plain".getBytes()),
    ICO("image/x-icon".getBytes()),
    GIF("image/gif".getBytes()),
    JS("application/javascript".getBytes()),
    JSON("application/json".getBytes());

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
    private final byte[] valueForResponse;

    ///
}
