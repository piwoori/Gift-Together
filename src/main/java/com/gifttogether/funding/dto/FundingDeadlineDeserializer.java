package com.gifttogether.funding.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.gifttogether.common.exception.BadRequestException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class FundingDeadlineDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final String INVALID_DEADLINE =
            "마감일(expiredAt)은 시간대 오프셋이 없는 ISO 8601 한국 시간이어야 합니다.";

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            throw new BadRequestException(INVALID_DEADLINE);
        }

        try {
            return LocalDateTime.parse(parser.getText());
        } catch (DateTimeParseException e) {
            throw new BadRequestException(INVALID_DEADLINE);
        }
    }
}
