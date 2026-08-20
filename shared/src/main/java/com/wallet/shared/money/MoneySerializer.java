package com.wallet.shared.money;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

/**
 * Jackson serializer for {@link Money} so it is always rendered as "100.50 USD".
 * Useful when Money is nested in records that have other Jackson annotations
 * and we want to bypass the {@link Money#json()} @JsonValue path.
 */
public class MoneySerializer extends ValueSerializer<Money> {

    @Override
    public void serialize(Money value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeString(value == null ? null : value.amount().toPlainString() + " " + value.currency());
    }
}
