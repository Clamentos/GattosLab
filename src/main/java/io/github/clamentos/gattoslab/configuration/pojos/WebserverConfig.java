package io.github.clamentos.gattoslab.configuration.pojos;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class WebserverConfig {

    ///
    private final int serverPort;
    private final String host;

    ///
}
