package io.github.clamentos.gattoslab.utils;

///
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

///.
import java.io.IOException;

///
public final class MutableLongSerializer extends JsonSerializer<MutableLong> {

    ///
    @Override
    public void serialize(final MutableLong value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {

        gen.writeNumber(value.getValue());
    }

    ///
}
