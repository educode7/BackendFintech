package com.wallet.shared.money;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Jackson serializer for {@link Money} so it is always rendered as "100.50 USD".
 * Useful when Money is nested in records that have other Jackson annotations
 * and we want to bypass the {@link Money#json()} @JsonValue path.
 */
public class MoneySerializer extends JsonSerializer<Money> {

    @Override
    public void serialize(Money value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(value == null ? null : value.amount().toPlainString() + " " + value.currency());
    }
}
