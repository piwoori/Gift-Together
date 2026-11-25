package com.gifttogether.contribution.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.gifttogether.common.exception.BadRequestException;

import java.io.IOException;

public class ContributionAmountDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            throw new BadRequestException("참여 금액은 정수로 입력해 주세요.");
        }
        return parser.getLongValue();
    }
}
