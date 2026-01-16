package io.github.clamentos.gattoslab.session;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum SessionRole {

    ///
    ADMIN("admin");

    ///
    private final String propertySection;

    ///
}
