package io.github.clamentos.gattoslab.configuration.dynamic.entities;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import io.github.clamentos.gattoslab.exceptions.CauseContainer;
import io.github.clamentos.gattoslab.exceptions.ValidationException;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.net.InetAddress;

///..
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@EqualsAndHashCode
@Getter

///
public final class BlacklistIpEntry {

    ///
    private static final String SOURCE = "BlacklistIpEntry.<init>";

    ///
    private final byte[] start;
    private final byte[] end;

    ///
    @JsonCreator
    public BlacklistIpEntry(@JsonProperty("start") final String start, @JsonProperty("end") final String end) throws ValidationException {

        if(start == null || start.isBlank()) throw new ValidationException("Field 'start' cannot be null nor blank", SOURCE);
        if(end == null || end.isBlank()) throw new ValidationException("Field 'end' cannot be null nor blank", SOURCE);

        try {

            this.start = InetAddress.ofLiteral(start).getAddress();
            this.end = InetAddress.ofLiteral(end).getAddress();
        }

        catch(final IllegalArgumentException exc) {

            throw new ValidationException(GenericUtils.WRAPPED_EXCEPTION_MSG, new CauseContainer(SOURCE, exc));
        }
    }

    ///
}
