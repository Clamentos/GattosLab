package io.github.clamentos.gattoslab.session;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///.
import org.jspecify.annotations.NonNull;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum SessionRole {

    ///
    ADMIN("admin");

    ///
    @NonNull private final String propertySection;

    ///
}
