package io.github.clamentos.gattoslab.configuration.pojos;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SessionConfig {

    ///
    private final String cleanSchedule;
    private final String cookieName;
    private final int duration;
    private final boolean enabled;
    private final int loginDelay;

    ///
}
