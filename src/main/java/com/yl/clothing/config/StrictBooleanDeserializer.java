package com.yl.clothing.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/** Require a JSON boolean for opt-in actions instead of coercing text or numbers. */
public class StrictBooleanDeserializer extends JsonDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_TRUE)) return true;
        if (parser.hasToken(JsonToken.VALUE_FALSE)) return false;
        return (Boolean) context.handleUnexpectedToken(Boolean.class, parser);
    }
}
