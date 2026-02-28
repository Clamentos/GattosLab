package io.github.clamentos.gattoslab.utils;

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
    HTML("text/html"),
    CSS("text/css"),
    PNG("image/png"),
    JPG("image/jpg"),
    JPEG("image/jpeg"),
    SVG("image/svg+xml"),
    WEBP("image/webp"),
    XML("application/xml"),
    TXT("text/plain"),
    ICO("image/x-icon"),
    GIF("image/gif"),
    JS("application/javascript"),
    JSON("application/json");

    ///
    private final String mimeValue;

    ///
}
