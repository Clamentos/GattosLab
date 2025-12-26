package io.github.clamentos.gattoslab.utils;

///
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

///
public final class MutableLongSerializer extends ValueSerializer<MutableLong> {

    ///
    @Override
    public void serialize(MutableLong value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {

        gen.writeNumber(value.getValue());
    }

    ///
}
